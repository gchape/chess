package io.gchape.github.model.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class Message {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Get the message type identifier
     */
    public abstract String getType();

    /**
     * Convert this message to JSON string
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Create a Message from JSON string based on type
     */
    public static Message fromJson(String json) {
        // First parse to get the type
        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        String type = jsonObject.get("type") != null ? jsonObject.get("type").getAsString() : null;

        if (type == null) {
            throw new IllegalArgumentException("Message must have a 'type' field");
        }

        // Create appropriate message type based on the type field
        switch (type.toUpperCase()) {
            case "LOGIN":
                return gson.fromJson(json, LoginMessage.class);
            case "REGISTER":
                return gson.fromJson(json, RegisterMessage.class);
            case "JOIN_GAME":
                return gson.fromJson(json, JoinGameMessage.class);
            case "MOVE":
                return gson.fromJson(json, MoveMessage.class);
            case "SURRENDER":
                return gson.fromJson(json, SurrenderMessage.class);
            case "SPECTATE":
                return gson.fromJson(json, SpectateMessage.class);
            case "DISCONNECTION":
                return gson.fromJson(json, DisconnectionMessage.class);
            case "AUTH_RESPONSE":
                return gson.fromJson(json, AuthResponseMessage.class);
            case "GAME_STATE":
                return gson.fromJson(json, GameStateMessage.class);
            case "ERROR":
                return gson.fromJson(json, ErrorMessage.class);
            default:
                throw new IllegalArgumentException("Unknown message type: " + type);
        }
    }
}