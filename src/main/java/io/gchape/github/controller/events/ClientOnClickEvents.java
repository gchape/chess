package io.gchape.github.controller.events;

import javafx.scene.input.MouseEvent;

public interface ClientOnClickEvents {
    void onLoginClicked(MouseEvent e);
    void onGuestClicked(MouseEvent e);
    void onRegisterClicked(MouseEvent e);

    void onSquareClicked(MouseEvent e);
}
