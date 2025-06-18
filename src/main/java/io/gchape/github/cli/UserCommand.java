package io.gchape.github.cli;

import io.gchape.github.controller.ClientController;
import io.gchape.github.controller.ServerController;
import io.gchape.github.model.ClientModel;
import io.gchape.github.model.ServerModel;
import io.gchape.github.view.ClientView;
import io.gchape.github.view.ServerView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.util.Objects;

@Command(group = "Cli Commands")
public class UserCommand {
    private Stage stage;

    @Command(command = "start", group = "Cli Commands")
    public void start(@Option(required = true, longNames = "user-agent") final String userAgent,
                      @Option(longNames = "host", defaultValue = "localhost") final String host,
                      @Option(longNames = "port", defaultValue = "8080") final int port) {

        Platform.runLater(() -> {
            this.stage = new Stage();
            stage.setTitle("ChessFX");
            stage.setHeight(600);
            stage.setWidth(800);

            switch (userAgent) {
                case "client" -> startClient(host, port);
                case "server" -> startServer(host, port);
            }

            stage.show();
        });
    }

    private void startServer(final String host, final int port) {
        ServerView serverView = ServerView.INSTANCE;
        var serverController = new ServerController(serverView, new ServerModel());

        serverController.startServer(host, port);

        var scene = new Scene(serverView.view());
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            serverController.stopServer();

            System.exit(0);
        });

        addExternalCss(scene, "server-view.css");
    }

    private void startClient(final String host, final int port) {
        var clientView = new ClientView();
        var clientController = new ClientController(clientView, new ClientModel());

        clientController.startClient(host, port);

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
