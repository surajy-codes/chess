package com.yourapp.chess.model.dto;

import java.util.UUID;

public record MoveRequest(UUID userId, String from, String to, String promotion) {}