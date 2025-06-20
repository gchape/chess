package io.gchape.github.cli.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.jline.PromptProvider;

@Configuration
public class CliConfig {

    @Bean
    PromptProvider promptProvider() {
        return () -> new AttributedString(
                "CHESS-CLI:> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)
        );
    }
}
