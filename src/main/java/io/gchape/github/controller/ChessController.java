package io.gchape.github.controller;

import io.gchape.github.model.*;
import io.gchape.github.model.message.*;
import io.gchape.github.service.DatabaseService;
import io.gchape.github.service.GameSessionManager;
import io.gchape.github.view.GameWindow;
import io.gchape.github.controller.client.ClientController;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChessController {
    private GameState gameState;
    private GameWindow view;
    private String gameMode;
    private ClientController clientController; // For network communication

    // Network components
    private DatabaseService databaseService;
    private GameSessionManager sessionManager;
    private Long currentGameId;
    private Long currentUserId;
    private boolean isNetworkMode = false;

    // Message handler interface
    public interface MessageHandler {
        void sendMessage(Message message);
    }
    private MessageHandler messageHandler;

    public ChessController() {
        this.gameState = new GameState();
    }

    // Network constructor
    public ChessController(DatabaseService dbService, GameSessionManager sessionMgr) {
        this();
        this.databaseService = dbService;
        this.sessionManager = sessionMgr;
        this.isNetworkMode = true;
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public void setView(GameWindow view) {
        this.view = view;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    /**
     * Check if a piece can be selected at the given position
     */
    public boolean canSelectPiece(Position position) {
        Piece piece = getPieceAt(position);
        if (piece == null) {
            return false;
        }

        // In network mode, only allow selecting pieces of current player's color
        if (isNetworkMode) {
            // Add logic to check if it's the player's turn
            return piece.getColor() == gameState.getCurrentTurn();
        }

        // In local mode, allow selecting pieces of the current turn
        return piece.getColor() == gameState.getCurrentTurn();
    }

    /**
     * Handle surrender request
     */
    public void handleSurrender() {
        if (gameState.isGameOver()) {
            return;
        }

        // Determine winner (opposite of current player)
        PieceColor winner = gameState.getCurrentTurn() == PieceColor.WHITE ?
                PieceColor.BLACK : PieceColor.WHITE;

        gameState.setGameResult(winner + " wins by resignation");
        gameState.setGameOver(true);

        if (view != null) {
            view.showGameOver(gameState.getGameResult());
        }

        // In network mode, send surrender message
        if (isNetworkMode && messageHandler != null) {
            SurrenderMessage surrenderMsg = new SurrenderMessage();
            surrenderMsg.setGameId(currentGameId);
            surrenderMsg.setUserId(currentUserId);
            messageHandler.sendMessage(surrenderMsg);
        }
    }

    /**
     * Handle new game request
     */
    public void handleNewGame() {
        // Reset game state
        gameState = new GameState();

        if (view != null) {
            view.refreshBoard();
            view.updateStatus("New game started - White's turn");
        }

        // In network mode, this might need server coordination
        if (isNetworkMode && clientController != null) {
            // Send new game request to server
            // This would need to be implemented based on your server protocol
        }
    }

    /**
     * Handle PGN export
     */
    public void handleExportPGN(String filePath) {
        try {
            // Generate PGN string from current game
            String pgn = generatePGN();

            // Write to file
            java.nio.file.Files.write(
                    java.nio.file.Paths.get(filePath),
                    pgn.getBytes()
            );

            System.out.println("Game exported to: " + filePath);
        } catch (Exception e) {
            System.err.println("Failed to export PGN: " + e.getMessage());
        }
    }

    /**
     * Handle PGN import
     */
    public void handleImportPGN(String filePath) {
        try {
            // Read PGN file
            String pgn = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filePath)
            ));

            // Parse and load game
            loadGameFromPGN(pgn);

            if (view != null) {
                view.refreshBoard();
            }

            System.out.println("Game imported from: " + filePath);
        } catch (Exception e) {
            System.err.println("Failed to import PGN: " + e.getMessage());
        }
    }

    /**
     * Generate PGN string from current game
     */
    private String generatePGN() {
        StringBuilder pgn = new StringBuilder();

        // PGN headers
        pgn.append("[Event \"Chess Game\"]\n");
        pgn.append("[Site \"Local\"]\n");
        pgn.append("[Date \"").append(java.time.LocalDate.now()).append("\"]\n");
        pgn.append("[White \"White Player\"]\n");
        pgn.append("[Black \"Black Player\"]\n");

        if (gameState.isGameOver()) {
            String result = "1/2-1/2"; // Draw by default
            if (gameState.getGameResult().contains("White")) {
                result = "1-0";
            } else if (gameState.getGameResult().contains("Black")) {
                result = "0-1";
            }
            pgn.append("[Result \"").append(result).append("\"]\n");
        } else {
            pgn.append("[Result \"*\"]\n");
        }

        pgn.append("\n");

        // Move history
        List<Move> moveHistory = gameState.getBoard().getMoveHistory();
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                pgn.append((i / 2 + 1)).append(". ");
            }
            pgn.append(generateAlgebraicNotation(moveHistory.get(i))).append(" ");
        }

        if (gameState.isGameOver()) {
            String result = "*";
            if (gameState.getGameResult().contains("White")) {
                result = "1-0";
            } else if (gameState.getGameResult().contains("Black")) {
                result = "0-1";
            } else if (gameState.getGameResult().contains("Draw")) {
                result = "1/2-1/2";
            }
            pgn.append(result);
        }

        return pgn.toString();
    }

    /**
     * Load game from PGN string
     */
    private void loadGameFromPGN(String pgn) {
        // Reset game state
        gameState = new GameState();

        // Parse PGN moves (simplified implementation)
        String[] lines = pgn.split("\n");
        StringBuilder moves = new StringBuilder();

        for (String line : lines) {
            if (!line.startsWith("[") && !line.trim().isEmpty()) {
                moves.append(line).append(" ");
            }
        }

        String[] moveTokens = moves.toString().trim().split("\\s+");

        for (String token : moveTokens) {
            // Skip move numbers and results
            if (token.matches("\\d+\\.") || token.equals("1-0") ||
                    token.equals("0-1") || token.equals("1/2-1/2") || token.equals("*")) {
                continue;
            }

            // Parse and make move (simplified - would need full PGN parser)
            try {
                Move move = parseAlgebraicNotation(token);
                if (move != null) {
                    gameState.makeMove(move);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse move: " + token);
            }
        }
    }

    /**
     * Parse algebraic notation to Move object (simplified)
     */
    private Move parseAlgebraicNotation(String notation) {
        // This is a simplified implementation
        // A full implementation would need to handle all PGN notation cases

        // Remove check/checkmate symbols
        notation = notation.replace("+", "").replace("#", "");

        // Handle castling
        if (notation.equals("O-O") || notation.equals("O-O-O")) {
            // Handle castling moves
            return null; // Placeholder - implement castling logic
        }

        // For now, return null - full PGN parsing is complex
        return null;
    }

    // Handle network move
    public void handleNetworkMove(MoveMessage moveMsg) {
        try {
            Position from = Position.fromAlgebraic(moveMsg.getFromPosition());
            Position to = Position.fromAlgebraic(moveMsg.getToPosition());

            if (makeMove(from, to)) {
                // Record move in database
                recordMoveInDatabase(moveMsg);

                // Broadcast game state
                broadcastGameState();
            }
        } catch (Exception e) {
            sendErrorMessage("Invalid move: " + e.getMessage());
        }
    }

    private void recordMoveInDatabase(MoveMessage moveMsg) {
        if (!isNetworkMode || databaseService == null) return;

        try {
            Move lastMove = gameState.getBoard().getLastMove();
            if (lastMove != null) {
                databaseService.recordMove(
                        currentGameId,
                        gameState.getMoveNumber(),
                        gameState.getCurrentTurn().toString(),
                        moveMsg.getFromPosition(),
                        moveMsg.getToPosition(),
                        moveMsg.getPieceType(),
                        lastMove.getCapturedPiece() != null ? lastMove.getCapturedPiece().getClass().getSimpleName() : null,
                        getSpecialMoveType(lastMove),
                        moveMsg.getPromotionPiece(),
                        gameState.getBoard().isInCheck(gameState.getCurrentTurn()),
                        gameState.getBoard().isCheckmate(gameState.getCurrentTurn()),
                        generateAlgebraicNotation(lastMove)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void broadcastGameState() {
        if (messageHandler == null) return;

        GameStateMessage stateMsg = new GameStateMessage();
        stateMsg.setGameId(currentGameId);
        stateMsg.setBoardState(generateFEN());
        stateMsg.setCurrentTurn(gameState.getCurrentTurn().toString());
        stateMsg.setCheck(gameState.getBoard().isInCheck(gameState.getCurrentTurn()));
        stateMsg.setCheckmate(gameState.getBoard().isCheckmate(gameState.getCurrentTurn()));
        stateMsg.setStalemate(gameState.getBoard().isStalemate(gameState.getCurrentTurn()));
        stateMsg.setMoveNumber(gameState.getMoveNumber());

        if (gameState.isGameOver()) {
            stateMsg.setGameResult(determineGameResult());
            stateMsg.setGameEnded(true);
        }

        messageHandler.sendMessage(stateMsg);
    }

    // Main makeMove method - handles both local and network moves
    public boolean makeMove(Position fromPosition, Position toPosition) {
        // Get the piece at the source position
        Board board = gameState.getBoard();
        Piece piece = board.getPiece(fromPosition);

        if (piece == null || piece.getColor() != gameState.getCurrentTurn()) {
            return false;
        }

        // Find a legal move that matches the from and to positions
        List<Move> legalMoves = getLegalMovesForPiece(piece);
        Move moveToMake = null;

        for (Move move : legalMoves) {
            if (move.getFrom().equals(fromPosition) && move.getTo().equals(toPosition)) {
                moveToMake = move;
                break;
            }
        }

        if (moveToMake == null) {
            // Create a basic move using Builder pattern
            moveToMake = new Move.Builder()
                    .from(fromPosition)
                    .to(toPosition)
                    .piece(piece)
                    .capturedPiece(board.getPiece(toPosition))
                    .build();

            // Validate this move is actually legal
            if (!legalMoves.contains(moveToMake)) {
                return false;
            }
        }

        // Make the move
        boolean successful = gameState.makeMove(moveToMake);

        if (successful) {
            updateView();

            // Send network message if in network mode
            if (isNetworkMode && clientController != null) {
                clientController.sendMove(
                        fromPosition.toAlgebraic(),
                        toPosition.toAlgebraic(),
                        null // promotion piece
                );
            } else if (isNetworkMode && messageHandler != null) {
                MoveMessage moveMsg = new MoveMessage();
                moveMsg.setFromPosition(fromPosition.toAlgebraic());
                moveMsg.setToPosition(toPosition.toAlgebraic());
                moveMsg.setPieceType(piece.getClass().getSimpleName());
                moveMsg.setGameId(currentGameId);
                messageHandler.sendMessage(moveMsg);
            }

            // Check if the game is over
            if (gameState.isGameOver()) {
                if (view != null) {
                    view.showGameOver(gameState.getGameResult());
                }
                return true;
            }

            // If playing against computer, make computer move
            if (gameMode != null &&
                    ((gameMode.equals("Player vs Computer") && gameState.getCurrentTurn() == PieceColor.BLACK) ||
                            gameMode.equals("Computer vs Computer"))) {
                makeComputerMove();
            }
        }

        return successful;
    }

    /**
     * Makes a computer move using a simple AI.
     */
    private void makeComputerMove() {
        // Simple AI: choose a random legal move
        List<Move> legalMoves = getAllLegalMoves(gameState.getCurrentTurn());

        if (!legalMoves.isEmpty()) {
            Random random = new Random();
            int randomNum = random.nextInt(legalMoves.size());
            Move computerMove = legalMoves.get(randomNum);

            // Make the move after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(100); // 0.1 second delay
                    gameState.makeMove(computerMove);
                    updateView();

                    // Check if the game is over
                    if (gameState.isGameOver()) {
                        if (view != null) {
                            view.showGameOver(gameState.getGameResult());
                        }
                    } else if (gameMode != null && gameMode.equals("Computer vs Computer")) {
                        // Continue with next computer move
                        makeComputerMove();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Updates the view to reflect the current game state.
     */
    private void updateView() {
        if (view != null) {
            view.refreshBoard();

            // Update status message
            String statusMessage = gameState.getCurrentTurn().toString() + "'s turn";

            // Add check notification if applicable
            if (gameState.getBoard().isInCheck(gameState.getCurrentTurn())) {
                statusMessage += " (CHECK)";
            }

            view.updateStatus(statusMessage);
        }
    }

    /**
     * Gets the piece at the specified position.
     *
     * @param position The position to check
     * @return The piece at the position, or null if empty
     */
    public Piece getPieceAt(Position position) {
        return gameState.getBoard().getPiece(position);
    }

    /**
     * Gets all legal moves for the piece at the specified position.
     *
     * @param position The position of the piece
     * @return A list of positions representing legal moves
     */
    public List<Position> getLegalMovePositions(Position position) {
        Piece piece = getPieceAt(position);

        if (piece == null) {
            return new ArrayList<>();
        }

        List<Move> legalMoves = getLegalMovesForPiece(piece);
        List<Position> movePositions = new ArrayList<>();

        for (Move move : legalMoves) {
            movePositions.add(move.getTo());
        }

        return movePositions;
    }

    /**
     * Gets all legal moves for the specified piece.
     *
     * @param piece The piece to check
     * @return A list of legal moves
     */
    private List<Move> getLegalMovesForPiece(Piece piece) {
        return piece.getLegalMoves(gameState.getBoard());
    }

    /**
     * Gets all legal moves for the specified color.
     *
     * @param color The color to get moves for
     * @return A list of all legal moves
     */
    private List<Move> getAllLegalMoves(PieceColor color) {
        return gameState.getBoard().getLegalMoves(color);
    }

    /**
     * Gets the current board state.
     *
     * @return The chess board
     */
    public Board getBoard() {
        return gameState.getBoard();
    }

    /**
     * Gets the color of the player whose turn it is.
     *
     * @return The color of the current player
     */
    public PieceColor getCurrentTurn() {
        return gameState.getCurrentTurn();
    }

    /**
     * Checks if the game is over.
     *
     * @return True if the game is over, false otherwise
     */
    public boolean isGameOver() {
        return gameState.isGameOver();
    }

    // Utility methods
    private String generateFEN() {
        // Convert current board state to FEN notation
        // Implementation depends on your Board class
        return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"; // placeholder
    }

    private String generateAlgebraicNotation(Move move) {
        // Generate standard chess notation
        return move.toString(); // placeholder - implement proper algebraic notation
    }

    private String getSpecialMoveType(Move move) {
        if (move.isCastling()) return "CASTLING";
        if (move.isEnPassant()) return "EN_PASSANT";
        if (move.isPromotion()) return "PROMOTION";
        return null;
    }

    private String determineGameResult() {
        if (gameState.getBoard().isCheckmate(PieceColor.WHITE)) return "BLACK_WIN";
        if (gameState.getBoard().isCheckmate(PieceColor.BLACK)) return "WHITE_WIN";
        if (gameState.getBoard().isStalemate(gameState.getCurrentTurn())) return "DRAW";
        return "ONGOING";
    }

    private void sendErrorMessage(String error) {
        if (messageHandler != null) {
            ErrorMessage errorMsg = new ErrorMessage();
            errorMsg.setErrorCode("MOVE_ERROR");
            errorMsg.setErrorMessage(error);
            messageHandler.sendMessage(errorMsg);
        }
    }
    public void surrender() {
        handleSurrender();
    }

    /**
     * Handle disconnect request (called from GameWindow)
     */
    public void disconnect() {
        handleDisconnect();
    }

    /**
     * Start a new game with specified mode
     */
    public void startNewGame(String gameMode) {
        this.gameMode = gameMode;

        // Reset game state
        gameState = new GameState();

        if (view != null) {
            view.refreshBoard();
            view.updateStatus("New game started - White's turn");
        }

        // In network mode, notify server
        if (isNetworkMode && messageHandler != null) {
            // Send new game request to server
            // You'll need to implement NewGameMessage class
            try {
                Message newGameMsg = new Message() {
                    @Override
                    public String getType() { return "NEW_GAME"; }

                    @Override
                    public String toJson() {
                        return "{\"type\":\"NEW_GAME\",\"gameMode\":\"" + gameMode + "\"}";
                    }
                };
                messageHandler.sendMessage(newGameMsg);
            } catch (Exception e) {
                System.err.println("Failed to send new game request: " + e.getMessage());
            }
        }
    }

    /**
     * Connect to server for network play
     */
    public boolean connectToServer(String host, int port) {
        try {
            if (clientController != null) {
                clientController.startClient(host, port);
                isNetworkMode = true;
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle disconnect from server
     */
    private void handleDisconnect() {
        try {
            isNetworkMode = false;
            currentGameId = null;
            currentUserId = null;

            if (clientController != null) {
                clientController.handleDisconnect();
            }

            if (view != null) {
                view.updateStatus("Disconnected from server");
            }
        } catch (Exception e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    /**
     * Handle received game state from server
     */
    public void handleGameStateUpdate(GameStateMessage gameStateMsg) {
        try {
            // Update local game state based on server message
            if (gameStateMsg.isGameEnded()) {
                gameState.setGameOver(true);
                gameState.setGameResult(gameStateMsg.getGameResult());

                if (view != null) {
                    view.showGameOver(gameStateMsg.getGameResult());
                }
            }

            // Update board if FEN is provided
            if (gameStateMsg.getBoardState() != null) {
                // You'll need to implement FEN parsing in your Board class
                // gameState.getBoard().loadFromFEN(gameStateMsg.getBoardState());
            }

            // Update current turn
            if (gameStateMsg.getCurrentTurn() != null) {
                PieceColor turn = PieceColor.valueOf(gameStateMsg.getCurrentTurn());
                gameState.setCurrentTurn(turn);
            }

            // Update view
            updateView();

        } catch (Exception e) {
            System.err.println("Failed to handle game state update: " + e.getMessage());
        }
    }

    /**
     * Handle move received from server
     */
    public void handleServerMove(MoveMessage moveMsg) {
        try {
            Position from = Position.fromAlgebraic(moveMsg.getFromPosition());
            Position to = Position.fromAlgebraic(moveMsg.getToPosition());

            // Make the move without sending it back to server
            boolean oldNetworkMode = isNetworkMode;
            isNetworkMode = false; // Temporarily disable network mode to avoid echo

            boolean success = makeMove(from, to);

            isNetworkMode = oldNetworkMode; // Restore network mode

            if (!success) {
                System.err.println("Failed to make server move: " + moveMsg.getFromPosition() + " to " + moveMsg.getToPosition());
            }

        } catch (Exception e) {
            System.err.println("Failed to handle server move: " + e.getMessage());
        }
    }
    // Getters and setters
    public void setCurrentGameId(Long gameId) { this.currentGameId = gameId; }
    public void setCurrentUserId(Long userId) { this.currentUserId = userId; }
    public Long getCurrentGameId() { return currentGameId; }
    public Long getCurrentUserId() { return currentUserId; }
}