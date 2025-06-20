package io.gchape.github;

import atlantafx.base.theme.PrimerDark;
import io.gchape.github.cli.Commands;
import javafx.application.Application;
import javafx.application.Platform;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "io.gchape.github.view",
        "io.gchape.github.model",
        "io.gchape.github.cli.config",
        "io.gchape.github.controller",
})
@CommandScan(basePackageClasses = Commands.class)
    public class ChessApplication extends SpringApplication {

    public static void main(String[] args) {
        Platform.startup(() ->
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet()));

        SpringApplication.run(ChessApplication.class, args);
    }
}
