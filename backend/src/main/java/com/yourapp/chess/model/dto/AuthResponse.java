package com.yourapp.chess.model.dto;

import java.util.UUID;

public record AuthResponse(String token, UUID userId, String username) {}