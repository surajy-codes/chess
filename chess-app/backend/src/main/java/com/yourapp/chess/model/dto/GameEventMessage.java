package com.yourapp.chess.model.dto;

public record GameEventMessage(String event, String message) {

    // event values:
    // OPPONENT_DISCONNECTED  - opponent's connection dropped, grace period started
    // OPPONENT_RECONNECTED   - opponent came back before grace period expired
    // GAME_OVER              - game ended (checkmate, draw, or abandonment)
}