open module io.gchape.github.chess {
    requires javafx.controls;
    requires java.desktop;
    requires atlantafx.base;

    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.shell.core;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    exports io.gchape.github;
    exports io.gchape.github.cli;
    exports io.gchape.github.view;
    exports io.gchape.github.model;
    exports io.gchape.github.controller;
}
