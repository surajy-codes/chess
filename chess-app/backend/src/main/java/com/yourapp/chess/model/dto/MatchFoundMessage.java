package com.yourapp.chess.model.dto;

import java.util.UUID;

public record MatchFoundMessage(UUID gameId, String color, String opponentUsername) {}