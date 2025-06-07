package io.gchape.github.controller.client;

import io.gchape.github.controller.server.Server;
import io.gchape.github.model.entity.ClientMode;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Closeable {
    private final SocketChannel client;
    private final Selector readSelector;
    private final Selector writeSelector;

    private final Queue<String> outgoing;
    private final ExecutorService executorService;

    private volatile ClientMode clientMode;

    public Client(final String host, final int port) {
        try {
            client = SocketChannel.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
            client.connect(new InetSocketAddress(host, port));

            client.configureBlocking(false);
            client.register(writeSelector, 0);
            client.register(readSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

            outgoing = new ConcurrentLinkedQueue<>();
            executorService = Executors.newFixedThreadPool(3);

            executorService.submit(this::watchConsole);
            executorService.submit(this::watchReadable);
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

    public void send(final String message) {
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
        String[] body =
                request.toString()
                        .trim()
                        .split("\\R", -1);

        if (clientMode == null) {
            clientMode = ClientMode.valueOf(body[0]);
            body = Arrays.copyOfRange(body, 1, body.length);

            if (clientMode == ClientMode.PLAYER) {
                executorService.submit(this::watchWritable);
            }

            System.out.println(clientMode);
        }

        System.out.println(Arrays.toString(body));
    }

    @Override
    public void close() throws IOException {
        client.close();

        readSelector.close();
        writeSelector.close();

        executorService.shutdownNow();
    }
}
