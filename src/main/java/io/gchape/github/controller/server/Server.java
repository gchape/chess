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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Closeable {
    private final IntegerProperty clientCountProperty;
    private final StringProperty serverUpdatesProperty;

    private final ByteBuffer log;
    private final ExecutorService executorService;
    private final ConcurrentHashMap.KeySetView<SocketChannel, Boolean> socketChannels;

    private Selector acceptSelector;
    private Selector clientSelector;
    private ServerSocketChannel server;

    public Server(final StringProperty serverUpdatesProperty,
                  final IntegerProperty clientCountProperty) {
        this.log = ByteBuffer.allocate(16 * 1024);
        this.socketChannels = ConcurrentHashMap.newKeySet();
        this.executorService = Executors.newFixedThreadPool(2);

        this.clientCountProperty = clientCountProperty;
        this.serverUpdatesProperty = serverUpdatesProperty;
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

            syncUpdates("Server started at={ %s }.".formatted(serverAddress), 0);

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

                        socketChannels.add(clientChannel);

                        syncState(clientChannel);

                        syncUpdates("New client connected at={ %s }."
                                .formatted(LocalTime.now()), 1);
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
        var message = StandardCharsets.UTF_8.decode(buffer).toString().trim();

        if (!message.isEmpty()) {
            synchronized (log) {
                if (log.remaining() < message.length()) {
                    log.clear();
                }

                log.put(("%s%n".formatted(message)).getBytes(StandardCharsets.UTF_8));
            }

            syncUpdates("Received={ %s } from client.".formatted(message), 0);

            broadcastMessage(message, clientChannel);
        }

        buffer.clear();
    }

    private void broadcastMessage(final String message, final SocketChannel sender) {
        var broadcastBuffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

        synchronized (socketChannels) {
            for (SocketChannel client : socketChannels) {
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
    }

    private void syncState(SocketChannel clientChannel) throws IOException {
        synchronized (log) {
            ByteBuffer snapshot = log.duplicate();
            snapshot.flip();

            while (snapshot.hasRemaining()) {
                byte[] chunk = new byte[Math.min(1024, snapshot.remaining())];
                snapshot.get(chunk);

                clientChannel.write(ByteBuffer.wrap(chunk));
            }
        }
    }

    private void closeClient(final SocketChannel clientChannel, final SelectionKey key) throws IOException {
        key.cancel();
        clientChannel.close();
        socketChannels.remove(clientChannel);

        syncUpdates("Client disconnected at={ %s }.".formatted(LocalTime.now()), -1);
    }

    private void syncUpdates(final String serverUpdate, final int payload) {
        Platform.runLater(() -> {
            serverUpdatesProperty.set(serverUpdate);

            switch (payload) {
                case 1 -> clientCountProperty.set(clientCountProperty.add(1).intValue());
                case -1 -> clientCountProperty.set(clientCountProperty.subtract(1).intValue());
                default -> {
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        Thread.currentThread().interrupt();

        socketChannels
                .parallelStream()
                .forEach(socketChannel -> {
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        clientSelector.close();
        acceptSelector.close();
        server.close();
        executorService.shutdownNow();
    }
}
