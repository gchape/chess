package io.gchape.github;

import atlantafx.base.theme.PrimerDark;
import io.gchape.github.controller.ServerController;
import io.gchape.github.controller.client.Client;
import io.gchape.github.controller.server.Server;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ServerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class ChessApplication extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

    private static void setOnCloseRequest(final Stage stage, final Server server) {
        stage.setOnCloseRequest((e) -> {
            try {
                server.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void setOnCloseRequest(final Stage stage, final Client client) {
        stage.setOnCloseRequest((e) -> {
            try {
                client.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Override
    public void start(Stage stage) {
        Map<String, String> params = getParameters().getNamed();
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        String agent = params.get("agent");
        int port = Integer.parseInt(params.get("port"));

        if (agent.equals("server")) {
            var serverView = ServerView.INSTANCE;
            var serverController = new ServerController(serverView, new ServerModel());
            var server = serverController.getServer();

            server.startServer("localhost", 8080);

            setOnCloseRequest(stage, server);

            stage.setScene(new Scene(serverView.view()));
            stage.show();
        } else if (agent.equals("client")) {
            var client = new Client("localhost", port);

            setOnCloseRequest(stage, client);
        }

        stage.setHeight(600);
        stage.setWidth(800);
    }
}
