package io.gchape.github.controller.handlers;

import javafx.scene.input.MouseEvent;

public interface MouseClickHandlers {
    void onLoginClicked(MouseEvent e);
    void onGuestClicked(MouseEvent e);
    void onRegisterClicked(MouseEvent e);

    void onSquareClicked(MouseEvent e);
}
