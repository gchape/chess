package io.gchape.github.controller;

import io.gchape.github.controller.server.Server;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ServerView;

import java.io.IOException;

public class ServerController {
    private final ServerView serverView;
    private final ServerModel serverModel;

    private final Server server;

    public ServerController(final ServerView serverView,
                            final ServerModel serverModel) {
        this.server = new Server();
        this.serverView = serverView;
        this.serverModel = serverModel;

        setupBindings();
    }

    private void setupBindings() {
        serverModel.serverStatusProperty()
                .bind(server.serverStatusProperty());
        serverModel.connectedClientsProperty()
                .bind(server.connectedClientsProperty());

        serverView.serverStatusProperty()
                .bind(serverModel.serverStatusProperty());

        serverView.connectedClientsProperty()
                .bind(serverModel.connectedClientsProperty());
    }

    public void startServer(final String host, final int port) {
        server.startServer(host, port);
    }

    public void stopServer() {
        try {
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
