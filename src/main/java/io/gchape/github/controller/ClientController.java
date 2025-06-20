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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientController implements MouseClickHandlers {
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

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
        try {
            setupBindings();
            setupEventHandlers();
            logger.info("Client controller initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize client controller", e);
            uiManager.showError("Failed to initialize application. Please restart.");
        }
    }

    private void setupBindings() {
        clientModel.registerEmailProperty().bindBidirectional(clientView.registerEmailProperty());
        clientModel.registerPasswordProperty().bindBidirectional(clientView.registerPasswordProperty());
        clientModel.registerUsernameProperty().bindBidirectional(clientView.registerUsernameProperty());
        clientModel.loginUsernameProperty().bindBidirectional(clientView.loginUsernameProperty());
        clientModel.loginPasswordProperty().bindBidirectional(clientView.loginPasswordProperty());
    }

    private void setupEventHandlers() {
        gameService.setOnGameStateChanged(() ->
                uiManager.updateBoard(gameService.getGameState()));

        networkManager.setMessageHandler(this::handleNetworkMessage);
    }

    @Override
    public void onLoginClicked(MouseEvent e) {
        try {
            logger.debug("Login button clicked");

            if (authService.handleLogin()) {
                handleSuccessfulLogin(e);
            }
        } catch (Exception ex) {
            logger.error("Error during login process", ex);
            uiManager.showError("Login failed due to an unexpected error. Please try again.");
            authService.clearFormManually();
        }
    }

    @Override
    public void onGuestClicked(MouseEvent e) {
        try {
            logger.debug("Guest button clicked");

            authService.clearFormManually();

            networkManager.connectAsGuest();
            uiManager.switchToGameView(e);

            logger.info("Successfully connected as guest");
        } catch (Exception ex) {
            logger.error("Error during guest connection", ex);
            uiManager.showError("Failed to connect as guest. Please try again.");
        }
    }

    @Override
    public void onRegisterClicked(MouseEvent e) {
        try {
            logger.debug("Register button clicked");

            if (authService.handleRegistration()) {
                handleSuccessfulRegistration();
            }
        } catch (Exception ex) {
            logger.error("Error during registration process", ex);
            uiManager.showError("Registration failed due to an unexpected error. Please try again.");
            authService.clearFormManually();
        }
    }

    @Override
    public void onSquareClicked(MouseEvent event) {
        try {
            Position position = uiManager.getPositionFromEvent(event);
            if (position == null) {
                logger.warn("Could not determine position from mouse event");
                return;
            }

            if (gameService.hasSelection()) {
                handleSquareClickWithSelection(position);
            } else {
                handleSquareClickWithoutSelection(position);
            }
        } catch (Exception e) {
            logger.error("Error handling square click", e);
            uiManager.showError("Error processing move. Please try again.");
        }
    }

    private void handleSquareClickWithSelection(Position position) {
        try {
            if (gameService.isSelectedPosition(position)) {
                deselectPiece();
            } else if (gameService.canSelectPiece(position)) {
                selectPiece(position);
            } else {
                attemptMove(position);
            }
        } catch (Exception e) {
            logger.error("Error handling square click with selection", e);
            deselectPiece(); // Clear selection on error
        }
    }

    private void handleSquareClickWithoutSelection(Position position) {
        try {
            if (gameService.canSelectPiece(position)) {
                selectPiece(position);
            }
        } catch (Exception e) {
            logger.error("Error handling square click without selection", e);
        }
    }

    private void selectPiece(Position position) {
        try {
            gameService.selectPiece(position);
            uiManager.highlightSelection(position, gameService.getValidMoves(position));
            logger.debug("Selected piece at position: {}", position);
        } catch (Exception e) {
            logger.error("Error selecting piece at position: {}", position, e);
            uiManager.showError("Error selecting piece. Please try again.");
        }
    }

    private void deselectPiece() {
        try {
            gameService.deselectPiece();
            uiManager.clearHighlights();
            logger.debug("Deselected piece");
        } catch (Exception e) {
            logger.error("Error deselecting piece", e);
        }
    }

    private void attemptMove(Position to) {
        try {
            Position from = gameService.getSelectedPosition();
            if (from == null) {
                logger.warn("Attempted to move with no selected position");
                return;
            }

            Move move = new Move(from, to);

            if (gameService.makeMove(move)) {
                networkManager.sendMove(move, gameService.getGameState());
                uiManager.updateBoard(gameService.getGameState());
                deselectPiece();
                logger.info("Move executed successfully: {} to {}", from, to);
            } else {
                logger.debug("Invalid move attempted: {} to {}", from, to);
                uiManager.showError("Invalid move. Please try a different move.");
            }
        } catch (Exception e) {
            logger.error("Error attempting move", e);
            uiManager.showError("Error processing move. Please try again.");
            deselectPiece();
        }
    }

    private void handleNetworkMessage(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                logger.warn("Received empty or null network message");
                return;
            }

            if (gameService.isInitializationMessage(message)) {
                gameService.handleInitialization(message);
                logger.debug("Handled initialization message");
            } else if (gameService.isMoveMessage(message)) {
                gameService.handleOpponentMove(message);
                uiManager.updateBoard(gameService.getGameState());
                logger.debug("Handled opponent move message");
            } else {
                logger.warn("Received unknown message type: {}", message);
            }
        } catch (Exception e) {
            logger.error("Error handling network message: {}", message, e);
            uiManager.showError("Error processing network message. Connection may be unstable.");
        }
    }

    private void handleSuccessfulLogin(MouseEvent e) {
        try {
            networkManager.connectAsPlayer();
            uiManager.switchToGameView(e);

            logger.info("Successfully logged in and connected as player");
        } catch (Exception ex) {
            logger.error("Error handling successful login", ex);
            uiManager.showError("Login successful but failed to start game. Please try again.");
        }
    }

    private void handleSuccessfulRegistration() {
        logger.info("Registration completed successfully");
    }

    public void shutdown() {
        try {
            logger.info("Shutting down client controller");
            networkManager.disconnect();
            gameService.cleanup();
            logger.info("Client controller shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    public void handleConnectionLost() {
        try {
            uiManager.showError("Connection to server lost. Please restart the application.");
            logger.warn("Connection to server lost");
        } catch (Exception e) {
            logger.error("Error handling connection lost", e);
        }
    }

    public void handleServerError(String errorMessage) {
        try {
            uiManager.showError("Server error: " + (errorMessage != null ? errorMessage : "Unknown error"));
            logger.error("Server error received: {}", errorMessage);
        } catch (Exception e) {
            logger.error("Error handling server error", e);
        }
    }
}