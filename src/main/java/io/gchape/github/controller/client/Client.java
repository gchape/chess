package io.gchape.github.controller.client;

import io.gchape.github.controller.server.Server;
import io.gchape.github.model.entity.ClientMode;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Closeable {
    private final SocketChannel client;
    private final Selector readSelector;
    private final Selector writeSelector;

    private final Queue<String> incoming;
    private final Queue<String> outgoing;
    private final ExecutorService executorService;

    private volatile ClientMode clientMode;
    private volatile boolean running = true;

    public Client(final String host, final int port) {
        try {
            client = SocketChannel.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
            client.connect(new InetSocketAddress(host, port));

            client.configureBlocking(false);
            client.register(writeSelector, 0);
            client.register(readSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

            incoming = new ConcurrentLinkedQueue<>();
            outgoing = new ConcurrentLinkedQueue<>();
            executorService = Executors.newFixedThreadPool(2);

            executorService.submit(this::watchReadable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void watchReadable() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (readSelector.select(1_000) == 0) continue;

                var keys = readSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        read(key);
                    }
                }
            }
        } catch (IOException e) {
            if (running) { // Only log if we're still supposed to be running
                System.err.println("Error in watchReadable: " + e.getMessage());
            }
            try {
                close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void watchWritable() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (writeSelector.select(1_000) == 0) continue;

                var keys = writeSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    if (key.isValid() && key.isWritable()) {
                        flush();
                        key.interestOps(0);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error in watchWritable: " + e.getMessage());
            }
            try {
                close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void send(final String message) {
        if (!running) return;

        outgoing.offer(message);

        var key = client.keyFor(writeSelector);
        if (key != null && key.isValid()) {
            key.interestOps(SelectionKey.OP_WRITE);
            writeSelector.wakeup();
        }
    }

    private void flush() throws IOException {
        String message;
        while ((message = outgoing.poll()) != null) {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }
        }
    }

    private void read(final SelectionKey key) throws IOException {
        var buffer = (ByteBuffer) key.attachment();
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            close();
            return;
        } else if (bytesRead == 0) {
            return;
        }

        buffer.flip();
        parse(Server.CHARSET.decode(buffer));
        buffer.clear();
    }

    private void parse(final CharBuffer request) {
        String fullMessage = request.toString().trim();

        String[] messages = fullMessage.split("\\n");

        for (String message : messages) {
            message = message.trim();
            if (message.isEmpty()) continue;

            if (clientMode == null) {
                // Parse initial client mode message
                String[] parts = message.split(":");
                if (parts.length >= 1) {
                    try {
                        clientMode = ClientMode.valueOf(parts[0]);
                        System.out.println("Client mode set to: " + clientMode);

                        if (clientMode == ClientMode.PLAYER) {
                            executorService.submit(this::watchWritable);
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid client mode: " + parts[0]);
                    }
                }
            }

            incoming.offer(message);
            System.out.println("Received message: " + message);
        }
    }

    public Queue<String> getIncoming() {
        return incoming;
    }

    @Override
    public void close() throws IOException {
        running = false;

        if (client != null && client.isOpen()) {
            client.close();
        }

        if (readSelector != null && readSelector.isOpen()) {
            readSelector.close();
        }

        if (writeSelector != null && writeSelector.isOpen()) {
            writeSelector.close();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
