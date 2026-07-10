package com.yourapp.chess.model;

import com.github.bhlangonijr.chesslib.Board;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Getter
public class GameSession {

    private final UUID gameId;
    private final UUID whiteUserId;
    private final UUID blackUserId;
    private final Board board = new Board();

    // current STOMP session IDs - updated on connect/reconnect, nulled on disconnect
    @Setter private String whiteSessionId;
    @Setter private String blackSessionId;

    // whether each player is currently connected
    @Setter private boolean whiteConnected = false;
    @Setter private boolean blackConnected = false;

    // handle to the grace-period timer so we can cancel it on reconnect
    @Setter private ScheduledFuture<?> disconnectTimer;

    public GameSession(UUID gameId, UUID whiteUserId, UUID blackUserId) {
        this.gameId = gameId;
        this.whiteUserId = whiteUserId;
        this.blackUserId = blackUserId;
    }

    public boolean isWhite(UUID userId) {
        return whiteUserId.equals(userId);
    }

    public boolean isBlack(UUID userId) {
        return blackUserId.equals(userId);
    }
}