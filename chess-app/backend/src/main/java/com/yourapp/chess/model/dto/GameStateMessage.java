package com.yourapp.chess.model.dto;

public record GameStateMessage(String fen, String lastMove, String turn, String status) {}