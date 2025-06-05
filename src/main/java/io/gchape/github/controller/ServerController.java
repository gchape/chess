package io.gchape.github.controller;

import io.gchape.github.controller.server.Server;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ServerView;

public class ServerController {
    private final ServerView serverView;
    private final ServerModel serverModel;

    private final Server server;

    public ServerController(final ServerView serverView,
                            final ServerModel serverModel,
                            final int port) {
        this.serverView = serverView;
        this.serverModel = serverModel;
        this.server = new Server(
                serverModel.serverUpdatesProperty(),
                serverModel.clientCountProperty(),
                port);

        setupBindings();
    }

    private void setupBindings() {
        serverView.serverUpdatesProperty()
                .bind(serverModel.serverUpdatesProperty());

        serverView.clientCountProperty()
                .bind(serverModel.clientCountProperty());
    }

    public Server getServer() {
        return server;
    }
}
