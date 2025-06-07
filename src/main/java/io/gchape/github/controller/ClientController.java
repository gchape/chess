package io.gchape.github.controller;

import io.gchape.github.controller.client.Client;
import io.gchape.github.view.ClientView;

import java.io.IOException;

public class ClientController {
    private final ClientView clientView;

    private Client client;

    public ClientController(ClientView clientView) {
        this.clientView = clientView;
    }

    public void startClient(final String host, final int port) {
        client = new Client(host, port);
    }

    public void closeClient() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
