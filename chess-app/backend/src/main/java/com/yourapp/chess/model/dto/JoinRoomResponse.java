package com.yourapp.chess.model.dto;

import java.util.UUID;

public record JoinRoomResponse(UUID roomId, String color, String opponentUsername) {}