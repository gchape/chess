package io.gchape.github.controller.server;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Closeable {
    private final ExecutorService executorService;
    private final IntegerProperty clientCountProperty;
    private final StringProperty serverUpdatesProperty;
    private final ConcurrentLinkedQueue<SocketChannel> connections;

    private Selector acceptSelector;
    private Selector clientSelector;
    private InetSocketAddress serverAddress;
    private volatile boolean running = true;
    private ServerSocketChannel serverSocketChannel;

    public Server(final StringProperty serverUpdatesProperty,
                  final IntegerProperty clientCountProperty,
                  final int port) {
        this.clientCountProperty = clientCountProperty;
        this.serverUpdatesProperty = serverUpdatesProperty;
        this.connections = new ConcurrentLinkedQueue<>();
        this.executorService = Executors.newFixedThreadPool(2);

        startServer(port);
    }

    private void startServer(final int port) {
        serverAddress = new InetSocketAddress("localhost", port);

        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(serverAddress);
            serverSocketChannel.configureBlocking(false);

            acceptSelector = Selector.open();
            serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

            clientSelector = Selector.open();

            Platform.runLater(() ->
                    serverUpdatesProperty.set("Server started at={ %s }.".formatted(serverAddress))
            );

            executorService.submit(this::acceptHandler);
            executorService.submit(this::clientIOHandler);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void acceptHandler() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (acceptSelector.select(1000) == 0) {
                    continue;
                }

                Set<SelectionKey> keys = acceptSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptNewClient();
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Accept handler error: " + e.getMessage());
            }
        }
    }

    private void acceptNewClient() throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(clientSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

            connections.offer(clientChannel);

            Platform.runLater(() -> {
                clientCountProperty.set(clientCountProperty.add(1).intValue());
                serverUpdatesProperty.set("New client connected at={ %s }."
                        .formatted(LocalTime.now()));
            });
        }
    }

    private void clientIOHandler() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (clientSelector.select(1000) == 0) {
                    continue;
                }

                Set<SelectionKey> keys = clientSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        if (key.isReadable()) {
                            handleRead(clientChannel, key);
                        }
                    } catch (IOException e) {
                        closeClient((SocketChannel) key.channel(), key);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Client I/O handler error: " + e.getMessage());
            }
        }
    }

    private void handleRead(final SocketChannel clientChannel,
                            final SelectionKey key) throws IOException {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        if (buffer == null) {
            buffer = ByteBuffer.allocate(1024);
            key.attach(buffer);
        }

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            closeClient(clientChannel, key);
            return;
        } else if (bytesRead == 0) {
            return;
        }

        buffer.flip();
        String message = StandardCharsets.UTF_8.decode(buffer).toString().trim();

        if (!message.isEmpty()) {
            Platform.runLater(() ->
                    serverUpdatesProperty.set("Received={ %s } from client.".formatted(message))
            );

            broadcastMessage(message, clientChannel);
        }

        buffer.clear();
    }

    private void broadcastMessage(final String message, final SocketChannel sender) {
        byte[] messageBytes = (message + "\n").getBytes(StandardCharsets.UTF_8);
        ByteBuffer broadcastBuffer = ByteBuffer.wrap(messageBytes);

        for (SocketChannel client : connections) {
            if (!client.equals(sender) && client.isConnected()) {
                try {
                    broadcastBuffer.rewind();
                    client.write(broadcastBuffer);
                } catch (IOException e) {
                    System.err.println("Error broadcasting to client: " + e.getMessage());
                }
            }
        }
    }

    private void closeClient(final SocketChannel clientChannel, final SelectionKey key) {
        try {
            connections.remove(clientChannel);
            key.cancel();
            clientChannel.close();

            Platform.runLater(() -> {
                clientCountProperty.set(clientCountProperty.subtract(1).intValue());
                serverUpdatesProperty.set("Client disconnected at={ %s }."
                        .formatted(LocalTime.now()));
            });
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        running = false;

        if (acceptSelector != null) {
            acceptSelector.wakeup();
        }
        if (clientSelector != null) {
            clientSelector.wakeup();
        }

        for (SocketChannel client : connections) {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }

        connections.clear();

        if (acceptSelector != null) {
            acceptSelector.close();
        }
        if (clientSelector != null) {
            clientSelector.close();
        }
        if (serverSocketChannel != null) {
            serverSocketChannel.close();
        }

        executorService.shutdownNow();
    }
}