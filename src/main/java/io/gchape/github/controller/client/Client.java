package io.gchape.github.controller.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

    private final StringBuilder message;
    private final Queue<String> outgoing;
    private final ExecutorService executorService;

    public Client(final String host, final int port) {
        try {
            client = SocketChannel.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
            client.connect(new InetSocketAddress(host, port));

            client.configureBlocking(false);
            client.register(writeSelector, 0);
            client.register(readSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

            message = new StringBuilder();
            outgoing = new ConcurrentLinkedQueue<>();
            executorService = Executors.newFixedThreadPool(3);

            executorService.submit(this::watchConsole);
            executorService.submit(this::watchReadable);
            executorService.submit(this::watchWritable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void watchReadable() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
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
            try {
                close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void watchWritable() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (writeSelector.select(1_000) == 0)
                    continue;

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
            try {
                close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void watchConsole() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                send(line + "\n");
            }
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void send(String message) {
        outgoing.offer(message);

        var key = client.keyFor(writeSelector);
        if (key != null) {
            key.interestOps(SelectionKey.OP_WRITE);
            writeSelector.wakeup();
        }
    }

    private void flush() throws IOException {
        String msg;
        while ((msg = outgoing.poll()) != null) {
            ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
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
        message.append(StandardCharsets.UTF_8.decode(buffer));
        buffer.clear();

        int newline;
        while ((newline = message.indexOf("\n")) != -1) {
            String line = message.substring(0, newline).trim();
            message.delete(0, newline + 1);

            parse(line);
        }
    }

    private void parse(final String message) {
        System.out.println(message);
    }

    @Override
    public void close() throws IOException {
        client.close();
        readSelector.close();
        writeSelector.close();
        executorService.shutdownNow();
    }
}
