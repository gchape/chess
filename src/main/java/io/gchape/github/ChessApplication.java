package io.gchape.github;

import atlantafx.base.theme.Dracula;
import io.gchape.github.cli.Commands;
import javafx.application.Application;
import javafx.application.Platform;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan(basePackageClasses = Commands.class)
public class ChessApplication extends SpringApplication {

    public static void main(String[] args) {
        Platform.startup(() ->
                Application.setUserAgentStylesheet(
                        new Dracula().getUserAgentStylesheet())
        );

        SpringApplication.run(ChessApplication.class, args);
    }
}
