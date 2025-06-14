package io.gchape.github.controller.server;

import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ServerView;

public class ServerController {
    private Server server;
    private final ServerView serverView;
    private final ServerModel serverModel;

    // Constructor that matches what ChessApplication expects
    public ServerController(ServerView serverView, ServerModel serverModel) {
        this.serverView = serverView;
        this.serverModel = serverModel;
        setupEventHandlers();
    }

    // Keep the no-argument constructor for compatibility if needed elsewhere
    public ServerController() {
        this.serverView = ServerView.INSTANCE;
        this.serverModel = new ServerModel();
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Set up start/stop button action
        serverView.setStartStopAction(() -> {
            if (serverView.isServerRunning()) {
                stopServer();
            } else {
                startServerWithDialog();
            }
        });
    }

    private void startServerWithDialog() {
        // You could add a dialog to get host/port, for now use defaults
        startServer("localhost", findAvailablePort());
    }

    public void startServer(String host, int port) {
        try {
            if (server != null && server.isRunning()) {
                serverView.addLogMessage("Server is already running");
                return;
            }

            server = new Server();

            // Bind server properties to view
            server.serverStatusProperty().addListener((obs, oldVal, newVal) ->
                    serverView.updateServerStatus(newVal));

            server.connectedClientsProperty().addListener((obs, oldVal, newVal) ->
                    serverView.updateConnectedClients(newVal.intValue()));

            // Start the server
            server.startServer(host, port);

            // Update view
            serverView.updateServerPort(String.valueOf(port));
            serverView.setServerRunning(true);
            serverView.addLogMessage("Server started on " + host + ":" + port);

        } catch (Exception e) {
            serverView.addLogMessage("Failed to start server: " + e.getMessage());
            serverView.updateServerStatus("Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            if (server != null) {
                server.close();
                server = null;
            }

            serverView.setServerRunning(false);
            serverView.updateServerStatus("Server stopped");
            serverView.updateConnectedClients(0);
            serverView.addLogMessage("Server stopped");

        } catch (Exception e) {
            serverView.addLogMessage("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int findAvailablePort() {
        // Try to find an available port starting from 8080
        for (int port = 8080; port <= 8090; port++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                return port;
            } catch (java.io.IOException e) {
                // Port is in use, try next one
            }
        }
        return 8080; // Default fallback
    }

    public boolean isServerRunning() {
        return server != null && server.isRunning();
    }

    public Server getServer() {
        return server;
    }
}
