package io.gchape.github.http.server;

import io.gchape.github.model.entity.ClientMode;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Closeable {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private final StringBuilder messageLog;

    private final IntegerProperty clientCount;
    private final StringProperty serverStatus;
    private final StringProperty respMessage;

    private final ExecutorService executorService;
    private final ConcurrentHashMap<SocketChannel, ClientMode> connections;

    private Selector clientSelector;
    private Selector acceptSelector;
    private ServerSocketChannel server;

    volatile private boolean player1;
    volatile private boolean player2;
    volatile private boolean running = true;

    public Server() {
        messageLog = new StringBuilder();
        connections = new ConcurrentHashMap<>();
        executorService = Executors.newFixedThreadPool(2);

        serverStatus = new SimpleStringProperty("");
        clientCount = new SimpleIntegerProperty(0);
        respMessage = new SimpleStringProperty("");
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public void startServer(final String host, final int port) {
        try {
            var serverAddress = new InetSocketAddress(host, port);

            server = ServerSocketChannel.open();
            server
                    .bind(serverAddress)
                    .configureBlocking(false);

            clientSelector = Selector.open();
            acceptSelector = Selector.open();
            server.register(acceptSelector, SelectionKey.OP_ACCEPT);

            updateState("Server started at={ %s }.".formatted(serverAddress), 0);

            executorService.submit(this::watchAcceptable);
            executorService.submit(this::watchReadable);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void watchAcceptable() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (acceptSelector.select(1000) == 0) {
                    continue;
                }

                var keys = acceptSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptNewClient((ServerSocketChannel) key.channel());
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error in watchAcceptable: " + e.getMessage());
            }
        }
    }

    private void acceptNewClient(final ServerSocketChannel acceptableChannel) throws IOException {
        Optional.ofNullable(acceptableChannel.accept())
                .ifPresent(clientChannel -> {
                    try {
                        clientChannel.configureBlocking(false);
                        clientChannel.register(clientSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

                        updateState("New client connected at={ %s }."
                                .formatted(LocalTime.now()), 1);

                        syncState(clientChannel);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void watchReadable() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (clientSelector.select(1000) == 0) {
                    continue;
                }

                var keys = clientSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    try {
                        var clientChannel = (SocketChannel) key.channel();

                        if (key.isReadable()) {
                            read(clientChannel, key);
                        }
                    } catch (IOException e) {
                        closeClient((SocketChannel) key.channel(), key);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error in watchReadable: " + e.getMessage());
            }
        }
    }

    private void read(final SocketChannel clientChannel,
                      final SelectionKey key) throws IOException {
        var buffer = (ByteBuffer) key.attachment();
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            closeClient(clientChannel, key);
            return;
        } else if (bytesRead == 0) {
            return;
        }

        buffer.flip();
        var message = CHARSET.decode(buffer).toString().trim();

        if (!message.isEmpty()) {
            synchronized (messageLog) {
                messageLog.append(message).append("\n");
            }

            updateState("Received={ %s } from client.".formatted(message), 0);
            respMessage.set("Server received: %s%n".formatted(message));

            broadcastMessage(message, clientChannel);
        }

        buffer.clear();
    }

    private void broadcastMessage(final String message, final SocketChannel sender) throws IOException {
        var broadcastBuffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

        synchronized (connections) {
            for (SocketChannel client : connections.keySet()) {
                if (!client.equals(sender) && client.isConnected()) {
                    try {
                        broadcastBuffer.rewind();
                        int bytesWritten = client.write(broadcastBuffer);

                        respMessage.set("Broadcasted %d bytes to client: %s%n".formatted(bytesWritten, message));
                    } catch (IOException e) {
                        respMessage.set("Failed to broadcast to client: %s%n".formatted(e.getMessage()));

                        var key = client.keyFor(clientSelector);
                        if (key != null) {
                            closeClient(client, key);
                        }
                    }
                }
            }
        }
    }

    private void syncState(SocketChannel clientChannel) throws IOException {
        ClientMode clientMode;
        String modeMessage;

        if (player1 && player2) {
            clientMode = ClientMode.SPECTATOR;
            modeMessage = "SPECTATOR:NONE\n";
        } else {
            clientMode = ClientMode.PLAYER;

            if (!player1) {
                player1 = true;
                modeMessage = "PLAYER:WHITE\n";
            } else {
                player2 = true;
                modeMessage = "PLAYER:BLACK\n";
            }
        }

        connections.put(clientChannel, clientMode);

        var modeBuffer = CHARSET.encode(modeMessage);
        while (modeBuffer.hasRemaining()) {
            clientChannel.write(modeBuffer);
        }

        respMessage.set("Sent to client: %s%n".formatted(modeMessage.trim()));

        synchronized (messageLog) {
            if (!messageLog.isEmpty()) {
                var history = messageLog.toString();
                var historyBuffer = CHARSET.encode(history);

                while (historyBuffer.hasRemaining()) {
                    clientChannel.write(historyBuffer);
                }

                respMessage.set("Sent message history to new client: %s%n".formatted(history.replace("\n", " | ")));
            }
        }
    }

    private void closeClient(final SocketChannel clientChannel, final SelectionKey key) throws IOException {
        key.cancel();

        var mode = connections.get(clientChannel);
        connections.remove(clientChannel);
        clientChannel.close();

        updateState("Client disconnected at={ %s }.".formatted(LocalTime.now()), -1);

        if (mode == ClientMode.PLAYER) {
            updateState("Player disconnected - game session affected at={ %s }".formatted(LocalTime.now()), 0);

            disconnectClients();
        }
    }

    private void updateState(final String serverUpdate, final int payload) {
        serverStatus.set(serverUpdate);

        switch (payload) {
            case 1 -> clientCount.set(clientCount.get() + 1);
            case -1 -> clientCount.set(clientCount.get() - 1);
            default -> {
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;

        disconnectClients();

        if (server != null && server.isOpen()) {
            server.close();
        }

        if (clientSelector != null && clientSelector.isOpen()) {
            clientSelector.close();
        }
        if (acceptSelector != null && acceptSelector.isOpen()) {
            acceptSelector.close();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void disconnectClients() {
        connections.keySet()
                .parallelStream()
                .forEach(socketChannel -> {
                    try {
                        socketChannel.close();
                    } catch (IOException ignored) {
                    }
                });

        connections.clear();
        clientCount.set(0);
        player1 = false;
        player2 = false;
    }

    public StringProperty respMessageProperty() {
        return respMessage;
    }
}