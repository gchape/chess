package io.gchape.github.controller;

import io.gchape.github.controller.handlers.MouseClickHandlers;
import io.gchape.github.model.ClientModel;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Position;
import io.gchape.github.model.service.AuthenticationService;
import io.gchape.github.model.service.GameService;
import io.gchape.github.model.service.NetworkManager;
import io.gchape.github.model.service.UIManager;
import io.gchape.github.view.ClientView;
import javafx.scene.input.MouseEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientController implements MouseClickHandlers {
    private final ClientView clientView;
    private final ClientModel clientModel;

    private final GameService gameService;
    private final AuthenticationService authService;
    private final NetworkManager networkManager;
    private final UIManager uiManager;

    @Autowired
    public ClientController(ClientView clientView, ClientModel clientModel, GameService gameService,
                            AuthenticationService authService, NetworkManager networkManager, UIManager uiManager) {
        this.clientView = clientView;
        this.clientModel = clientModel;
        this.gameService = gameService;
        this.uiManager = uiManager;
        this.networkManager = networkManager;
        this.authService = authService;

        initializeController();
    }

    private void initializeController() {
        setupBindings();
        setupEventHandlers();
    }

    private void setupBindings() {
        clientModel.emailProperty().bind(clientView.emailProperty());
        clientModel.passwordProperty().bind(clientView.passwordProperty());
        clientModel.usernameProperty().bind(clientView.usernameProperty());
    }

    private void setupEventHandlers() {
        gameService.setOnGameStateChanged(() ->
                uiManager.updateBoard(gameService.getGameState()));

        networkManager.setMessageHandler(this::handleNetworkMessage);
    }

    @Override
    public void onLoginClicked(MouseEvent e) {
        if (authService.handleLogin()) {
            handleSuccessfulLogin(e);
        }
    }

    @Override
    public void onGuestClicked(MouseEvent e) {
        networkManager.connectAsGuest();
        uiManager.switchToGameView(e);
    }

    @Override
    public void onRegisterClicked(MouseEvent e) {
        if (authService.handleRegistration()) {
            handleSuccessfulLogin(e);
        }
    }

    @Override
    public void onSquareClicked(MouseEvent event) {
        Position position = uiManager.getPositionFromEvent(event);
        if (position == null) return;

        if (gameService.hasSelection()) {
            handleSquareClickWithSelection(position);
        } else {
            handleSquareClickWithoutSelection(position);
        }
    }

    private void handleSquareClickWithSelection(Position position) {
        if (gameService.isSelectedPosition(position)) {
            deselectPiece();
        } else if (gameService.canSelectPiece(position)) {
            selectPiece(position);
        } else {
            attemptMove(position);
        }
    }

    private void handleSquareClickWithoutSelection(Position position) {
        if (gameService.canSelectPiece(position)) {
            selectPiece(position);
        }
    }

    private void selectPiece(Position position) {
        gameService.selectPiece(position);
        uiManager.highlightSelection(position, gameService.getValidMoves(position));
    }

    private void deselectPiece() {
        gameService.deselectPiece();
        uiManager.clearHighlights();
    }

    private void attemptMove(Position to) {
        Position from = gameService.getSelectedPosition();
        Move move = new Move(from, to);

        if (gameService.makeMove(move)) {
            networkManager.sendMove(move, gameService.getGameState());
            uiManager.updateBoard(gameService.getGameState());
            deselectPiece();
        }
    }

    private void handleNetworkMessage(String message) {
        if (gameService.isInitializationMessage(message)) {
            gameService.handleInitialization(message);
        } else if (gameService.isMoveMessage(message)) {
            gameService.handleOpponentMove(message);
            uiManager.updateBoard(gameService.getGameState());
        }
    }

    private void handleSuccessfulLogin(MouseEvent e) {
        networkManager.connectAsPlayer();
        uiManager.switchToGameView(e);
    }

    private void handleSuccessfulRegistration() {
        uiManager.showSuccess("Registration successful");
    }

    public void shutdown() {
        networkManager.disconnect();
        gameService.cleanup();
    }
}
