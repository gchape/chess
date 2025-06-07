package io.gchape.github.controller.server;

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
    private final IntegerProperty connectedClients;
    private final StringProperty serverStatus;

    private final ByteBuffer cache;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<SocketChannel, ClientMode> connections;

    private Selector clientSelector;
    private Selector acceptSelector;
    private ServerSocketChannel server;

    public Server() {
        this.connections = new ConcurrentHashMap<>();
        this.cache = ByteBuffer.allocate(16 * 1024);
        this.executorService = Executors.newFixedThreadPool(2);

        this.serverStatus = new SimpleStringProperty();
        this.connectedClients = new SimpleIntegerProperty(0);
    }

    public IntegerProperty connectedClientsProperty() {
        return connectedClients;
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
            while (!Thread.currentThread().isInterrupted()) {
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
            System.err.println(e.getMessage());
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
            while (!Thread.currentThread().isInterrupted()) {
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
            System.err.println(e.getMessage());
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
            synchronized (cache) {
                cache.put(CHARSET.encode("%s%n".formatted(message)));
            }

            updateState("Received={ %s } from client.".formatted(message), 0);

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
                        client.write(broadcastBuffer);
                    } catch (IOException e) {
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
        synchronized (cache) {
            ByteBuffer snapshot = cache.asReadOnlyBuffer().flip();

            ClientMode clientMode;
            if (connectedClients.get() > 2) {
                clientMode = ClientMode.SPECTATOR;
            } else {
                clientMode = ClientMode.PLAYER;
            }

            connections.put(clientChannel, clientMode);

            ByteBuffer mode = CHARSET.encode("%s%n".formatted(clientMode));
            ByteBuffer response = ByteBuffer.allocate(mode.remaining() + snapshot.remaining());

            response.put(mode);
            response.put(snapshot);

            response.flip();
            while (response.hasRemaining()) {
                clientChannel.write(response);
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
            updateState("Session closed at={ %s }".formatted(LocalTime.now()), 0);

            disconnectClients();
        }
    }

    private void updateState(final String serverUpdate, final int payload) {
        serverStatus.set(serverUpdate);

        switch (payload) {
            case 1 -> connectedClients.set(connectedClients.get() + 1);
            case -1 -> connectedClients.set(connectedClients.get() - 1);
            default -> {
            }
        }
    }

    @Override
    public void close() throws IOException {
        disconnectClients();

        server.close();
        clientSelector.close();
        acceptSelector.close();
        executorService.shutdownNow();
    }

    private void disconnectClients() {
        connections.keySet()
                .parallelStream()
                .forEach(socketChannel -> {
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        connections.clear();
        connectedClients.set(0);
    }
}
