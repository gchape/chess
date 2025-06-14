package io.gchape.github.controller.client;

import io.gchape.github.model.message.*;
import io.gchape.github.model.entity.ClientMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private final String host;
    private final int port;
    private final ClientController controller;

    private SocketChannel socketChannel;
    private Selector readSelector;
    private Selector writeSelector;
    private final BlockingQueue<String> outgoingMessages;
    private final ExecutorService executor;
    private final Gson gson;

    private volatile boolean connected = false;
    private ClientMode clientMode = ClientMode.PLAYER;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final StringBuilder messageBuffer = new StringBuilder();

    public Client(String host, int port, ClientController controller) throws IOException {
        this.host = host;
        this.port = port;
        this.controller = controller;
        this.outgoingMessages = new LinkedBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(2);

        // Configure Gson with better error handling
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        connect();
        startBackgroundThreads();
    }

    private void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        readSelector = Selector.open();
        writeSelector = Selector.open();

        // Connect to server
        socketChannel.connect(new InetSocketAddress(host, port));

        // Wait for connection to complete
        while (!socketChannel.finishConnect()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Connection interrupted", e);
            }
        }

        // Register for read operations
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);

        connected = true;
        System.out.println("Connected to server: " + host + ":" + port);
    }

    private void startBackgroundThreads() {
        // Background thread for reading messages
        executor.submit(this::watchReadable);

        // Background thread for writing messages
        executor.submit(this::watchWritable);
    }

    private void watchReadable() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                if (readSelector.select(1000) > 0) {
                    Iterator<SelectionKey> keys = readSelector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isReadable()) {
                            read(key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            handleConnectionError("Read error: " + e.getMessage());
        }
    }

    private void watchWritable() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                if (writeSelector.select(1000) > 0) {
                    Iterator<SelectionKey> keys = writeSelector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isWritable()) {
                            flush();
                        }
                    }
                }
            }
        } catch (Exception e) {
            handleConnectionError("Write error: " + e.getMessage());
        }
    }

    private void read(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            readBuffer.clear();

            int bytesRead = channel.read(readBuffer);

            if (bytesRead > 0) {
                readBuffer.flip();
                String data = StandardCharsets.UTF_8.decode(readBuffer).toString();
                messageBuffer.append(data);

                // Process complete messages
                processMessages();

            } else if (bytesRead == -1) {
                // Connection closed by server
                handleConnectionError("Server closed connection");
            }
        } catch (IOException e) {
            handleConnectionError("Error reading from server: " + e.getMessage());
        }
    }

    private void processMessages() {
        String buffer = messageBuffer.toString();
        String[] lines = buffer.split("\n");

        // Process all complete lines except the last one (which might be incomplete)
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                handleMessage(line);
            }
        }

        // Keep the last (potentially incomplete) line in the buffer
        if (lines.length > 0) {
            String lastLine = lines[lines.length - 1];
            if (buffer.endsWith("\n")) {
                // Last line is complete
                if (!lastLine.trim().isEmpty()) {
                    handleMessage(lastLine.trim());
                }
                messageBuffer.setLength(0);
            } else {
                // Last line is incomplete, keep it in buffer
                messageBuffer.setLength(0);
                messageBuffer.append(lastLine);
            }
        }
    }

    private void handleMessage(String message) {
        try {
            System.out.println("Received message: " + message);

            // Try to parse as JSON first
            if (message.startsWith("{") && message.endsWith("}")) {
                handleJsonMessage(message);
            } else {
                // Handle plain text messages
                System.out.println("Handling plain text message: " + message);
                handlePlainTextMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleJsonMessage(String message) {
        try {
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();

            // Safe get method that handles null values
            String messageType = getStringFromJson(jsonObject, "type");

            if (messageType == null) {
                // Check for error messages without type field
                if (jsonObject.has("errorCode")) {
                    handleServerError(jsonObject);
                    return;
                }

                // Check for other message patterns
                if (jsonObject.has("username") && jsonObject.has("password")) {
                    // This looks like login data echoed back, ignore or handle appropriately
                    System.out.println("Received login data echo, ignoring");
                    return;
                }

                System.err.println("Message missing 'type' field: " + message);
                return;
            }

            switch (messageType) {
                case "AUTH_RESPONSE":
                    handleAuthResponse(jsonObject);
                    break;
                case "GAME_STATE":
                    handleGameState(jsonObject);
                    break;
                case "MOVE":
                    handleMove(jsonObject);
                    break;
                case "ERROR":
                    handleError(jsonObject);
                    break;
                case "GAME_JOINED":
                    handleGameJoined(jsonObject);
                    break;
                case "DISCONNECTION":
                    handleDisconnection(jsonObject);
                    break;
                default:
                    System.out.println("Unknown message type: " + messageType);
                    break;
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON received: " + message);
            System.err.println("JSON parse error: " + e.getMessage());
        }
    }

    private void handlePlainTextMessage(String message) {
        // Handle messages like "PLAYER"
        switch (message.toUpperCase()) {
            case "PLAYER":
                // Server is indicating player mode
                setClientMode(ClientMode.PLAYER);
                System.out.println("Set client mode to PLAYER");
                break;
            case "SPECTATOR":
                setClientMode(ClientMode.SPECTATOR);
                System.out.println("Set client mode to SPECTATOR");
                break;
            default:
                // Forward to controller for other plain text messages
                if (controller != null) {
                    controller.handleMessage(message);
                }
                break;
        }
    }

    private void handleServerError(JsonObject jsonObject) {
        String errorCode = getStringFromJson(jsonObject, "errorCode");
        String errorMessage = getStringFromJson(jsonObject, "errorMessage");

        System.err.println("Server error - Code: " + errorCode + ", Message: " + errorMessage);

        if (controller != null) {
            controller.handleAuthFailure(errorMessage != null ? errorMessage : "Server error: " + errorCode);
        }
    }

    // Safe method to get string values from JSON with null checking
    private String getStringFromJson(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        }
        return null;
    }

    // Safe method to get boolean values from JSON with null checking
    private Boolean getBooleanFromJson(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsBoolean();
        }
        return null;
    }

    // Safe method to get long values from JSON with null checking
    private Long getLongFromJson(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsLong();
        }
        return null;
    }

    private void handleAuthResponse(JsonObject jsonObject) {
        Boolean success = getBooleanFromJson(jsonObject, "success");
        String message = getStringFromJson(jsonObject, "message");

        if (success != null && success) {
            Long userId = getLongFromJson(jsonObject, "userId");
            String username = getStringFromJson(jsonObject, "username");

            if (controller != null) {
                controller.setCurrentUserId(userId);
                controller.setCurrentUsername(username);
                controller.handleAuthSuccess();
            }
        } else {
            if (controller != null) {
                controller.handleAuthFailure(message != null ? message : "Authentication failed");
            }
        }
    }

    private void handleGameState(JsonObject jsonObject) {
        // Handle game state updates
        if (controller != null) {
            controller.handleGameStateUpdate(jsonObject.toString());
        }
    }

    private void handleMove(JsonObject jsonObject) {
        // Handle move updates
        if (controller != null) {
            controller.handleMoveReceived(jsonObject.toString());
        }
    }

    private void handleError(JsonObject jsonObject) {
        String errorMessage = getStringFromJson(jsonObject, "message");
        System.err.println("Server error: " + (errorMessage != null ? errorMessage : "Unknown error"));

        if (controller != null) {
            controller.handleAuthFailure(errorMessage != null ? errorMessage : "Unknown server error");
        }
    }

    private void handleGameJoined(JsonObject jsonObject) {
        if (controller != null) {
            controller.handleGameJoined();
        }
    }

    private void handleDisconnection(JsonObject jsonObject) {
        if (controller != null) {
            controller.handleConnectionLost();
        }
    }

    private void flush() {
        try {
            while (!outgoingMessages.isEmpty()) {
                String message = outgoingMessages.poll();
                if (message != null) {
                    ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

                    while (buffer.hasRemaining()) {
                        socketChannel.write(buffer);
                    }
                }
            }
        } catch (IOException e) {
            handleConnectionError("Error writing to server: " + e.getMessage());
        }
    }

    public void send(String message) {
        if (connected) {
            outgoingMessages.offer(message);
            System.out.println("Queued message for sending: " + message);
        } else {
            System.err.println("Cannot send message - not connected: " + message);
        }
    }

    public void sendJson(Object object) {
        if (connected) {
            try {
                String jsonMessage = gson.toJson(object);
                send(jsonMessage);
            } catch (Exception e) {
                System.err.println("Error serializing object to JSON: " + e.getMessage());
            }
        }
    }

    // Method to send login request with proper type
    public void sendLoginRequest(String username, String password) {
        JsonObject loginMessage = new JsonObject();
        loginMessage.addProperty("type", "LOGIN");
        loginMessage.addProperty("username", username);
        loginMessage.addProperty("password", password);
        send(loginMessage.toString());
    }

    private void handleConnectionError(String errorMessage) {
        System.err.println("Connection error: " + errorMessage);
        connected = false;

        if (controller != null) {
            controller.handleConnectionLost();
        }
    }

    public void close() throws IOException {
        connected = false;

        executor.shutdown();

        if (readSelector != null) {
            readSelector.close();
        }

        if (writeSelector != null) {
            writeSelector.close();
        }

        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public ClientMode getClientMode() {
        return clientMode;
    }

    public void setClientMode(ClientMode clientMode) {
        this.clientMode = clientMode;
    }
}