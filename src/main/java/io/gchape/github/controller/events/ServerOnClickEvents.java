package io.gchape.github.controller.events;

import javafx.scene.input.MouseEvent;

public interface ServerOnClickEvents {
    void onShowDatabaseClicked(MouseEvent e);

    void onTableSelected(String tableName);

    void onRefreshTableClicked(MouseEvent e);

    void onGoBackClicked(MouseEvent e);
}