package io.gchape.github;

import atlantafx.base.theme.PrimerDark;
import io.gchape.github.controller.ClientController;
import io.gchape.github.controller.ServerController;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ClientView;
import io.gchape.github.view.ServerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
            var sv = ServerView.INSTANCE;
            var sc = new ServerController(sv, new ServerModel());
            sc.startServer("localhost", 8080);

            stage.setOnCloseRequest(e -> sc.stopServer());
            stage.setScene(new Scene(sv.view()));
            stage.show();
        } else if (agent.equals("client")) {
            var cv = new ClientView();
            var cc = new ClientController(cv);
            cc.startClient("localhost", port);

            stage.setOnCloseRequest(e -> cc.closeClient());
            stage.setScene(new Scene(cv.view()));
            stage.show();
        }

        stage.setHeight(600);
        stage.setWidth(800);
    }
}
