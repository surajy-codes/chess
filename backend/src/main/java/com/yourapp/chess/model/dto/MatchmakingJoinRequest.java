package com.yourapp.chess.model.dto;

import java.util.UUID;

public record MatchmakingJoinRequest(UUID clientId, String username) {}