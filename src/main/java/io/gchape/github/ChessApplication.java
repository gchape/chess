package io.gchape.github;

import atlantafx.base.theme.PrimerDark;
import io.gchape.github.controller.ServerController;
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

    @Override
    public void start(Stage stage) {
        Map<String, String> params = getParameters().getNamed();
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        String agent = params.get("agent");
        int port = Integer.parseInt(params.get("port"));

        if (agent.equals("server")) {
            var serverView = ServerView.INSTANCE;
            var serverController = new ServerController(serverView, new ServerModel(), port);

            stage.setOnCloseRequest((e) -> {
                try {
                    serverController.getServer().close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            stage.setScene(new Scene(serverView.view()));
        } else if (agent.equals("client")) {
            // TODO
        }

        stage.setHeight(600);
        stage.setWidth(800);
        stage.show();
    }
}
