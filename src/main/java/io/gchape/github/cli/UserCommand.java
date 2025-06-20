package io.gchape.github.cli;

import io.gchape.github.controller.ClientController;
import io.gchape.github.controller.ServerController;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ClientView;
import io.gchape.github.view.ServerView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Command
@Component
public class UserCommand {
    private final ClientController clientController;
    private final ClientView clientView;

    private Stage stage;

    @Autowired
    public UserCommand(ClientView clientView, ClientController clientController) {
        this.clientView = clientView;
        this.clientController = clientController;
    }

    @Command(command = "start-server",
            description = "Start the chess server and launch the server UI.")
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

    @Command(command = "connect-client",
            description = "Connect to a running chess server as a client and launch the client UI.")
    public void connectClient() {
        Platform.runLater(() -> {
            this.stage = new Stage();
            stage.setTitle("ChessFX");
            stage.setHeight(600);
            stage.setWidth(800);

            runClient();

            stage.show();
        });
    }

    private void runServer() {
        ServerView serverView = ServerView.INSTANCE;
        var serverController = new ServerController(serverView, new ServerModel());

        var scene = new Scene(serverView.view());
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            serverController.stopServer();

            System.exit(0);
        });

        addExternalCss(scene, "server-view.css");

        serverController.startServer("localhost", 8080);
    }

    private void runClient() {
        clientView.setMouseClickHandlers(clientController);

        var scene = new Scene(clientView.view());
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            clientController.shutdown();

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
