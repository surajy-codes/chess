package com.yourapp.chess.model.dto;

import java.util.UUID;

public record CreateRoomResponse(UUID roomId, String color) {}