package io.gchape.github.cli;

import io.gchape.github.controller.ClientController;
import io.gchape.github.controller.ServerController;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ClientView;
import io.gchape.github.view.ServerView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.shell.command.annotation.Command;

import java.util.Objects;

@Command
public class UserCommand {
    private Stage stage;

    @Command(command = "start-server", description = "")
    public void startServer() {

        Platform.runLater(() -> {
            this.stage = new Stage();
            stage.setTitle("ChessFX");
            stage.setHeight(600);
            stage.setWidth(800);

            runServer();

            stage.show();
        });
    }

    @Command(command = "connect-client", description = "")
    public void connectClient() {
        Platform.runLater(() -> {
            this.stage = new Stage();
            stage.setTitle("ChessFX");
            stage.setHeight(800);
            stage.setWidth(800);

            runClient();

            stage.show();
        });
    }

    private void runServer() {
        ServerView serverView = ServerView.INSTANCE;
        var serverController = new ServerController(serverView, new ServerModel());

        serverController.startServer("localhost", 8080);

        var scene = new Scene(serverView.view());
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            serverController.stopServer();

            System.exit(0);
        });

        addExternalCss(scene, "server-view.css");
    }

    private void runClient() {
        var clientView = new ClientView();
        var clientController = new ClientController();
        clientView.setClientController(clientController);

        clientController.startClient("localhost", 8080);

        var scene = new Scene(clientView.view());
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            clientController.closeClient();

            System.exit(0);
        });

        addExternalCss(scene, "client-view.css");
    }

    private void addExternalCss(final Scene scene, final String path) {
        scene.getStylesheets()
                .add(Objects.requireNonNull(
                                getClass().getResource("/css/" + path))
                        .toExternalForm());
    }
}
