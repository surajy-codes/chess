package com.yourapp.chess.model.dto;

public record GameEventMessage(String event, String message, String data) {

    // convenience constructor for events that don't need extra data
    public GameEventMessage(String event, String message) {
        this(event, message, null);
    }
}