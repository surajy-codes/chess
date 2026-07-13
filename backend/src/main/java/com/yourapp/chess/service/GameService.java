package com.yourapp.chess.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.yourapp.chess.model.GameSession;
import com.yourapp.chess.model.dto.GameEventMessage;
import com.yourapp.chess.model.dto.GameStateMessage;
import com.yourapp.chess.model.entity.GameResult;
import com.yourapp.chess.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final int GRACE_PERIOD_SECONDS = 45;

    private final Map<UUID, GameSession> activeSessions = new ConcurrentHashMap<>();

    // reverse map: stompSessionId -> gameId, so disconnect events can find the game fast
    private final Map<String, UUID> sessionToGame = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;
    private final EloService eloService;

    // --- session lifecycle ---

    public void startSession(UUID gameId, UUID whiteUserId, UUID blackUserId) {
        activeSessions.put(gameId, new GameSession(gameId, whiteUserId, blackUserId));
    }

    // Called from GameSocketController when a player sends /app/game/{id}/rejoin
    public void registerPlayerSession(UUID gameId, UUID userId, String stompSessionId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) return;

        boolean wasDisconnected = session.isWhite(userId)
                ? !session.isWhiteConnected()
                : !session.isBlackConnected();

        boolean hadTimer = session.getDisconnectTimer() != null && !session.getDisconnectTimer().isDone();

        // cancel any running disconnect timer
        if (hadTimer) {
            session.getDisconnectTimer().cancel(false);
            session.setDisconnectTimer(null);
        }

        if (session.isWhite(userId)) {
            session.setWhiteSessionId(stompSessionId);
            session.setWhiteConnected(true);
        } else {
            session.setBlackSessionId(stompSessionId);
            session.setBlackConnected(true);
        }

        sessionToGame.put(stompSessionId, gameId);

        // it's a reconnect if they were disconnected AND a grace timer was running
        if (wasDisconnected && hadTimer) {
            broadcastEvent(gameId, new GameEventMessage("OPPONENT_RECONNECTED", "Your opponent reconnected."));
        }
    }

    // Called from SessionDisconnectEvent listener
    public void handleDisconnect(String stompSessionId) {
        UUID gameId = sessionToGame.remove(stompSessionId);
        if (gameId == null) return;

        GameSession session = activeSessions.get(gameId);
        if (session == null) return;

        boolean isWhite = stompSessionId.equals(session.getWhiteSessionId());
        boolean isBlack = stompSessionId.equals(session.getBlackSessionId());

        if (!isWhite && !isBlack) return;

        if (isWhite) session.setWhiteConnected(false);
        if (isBlack) session.setBlackConnected(false);

        broadcastEvent(gameId, new GameEventMessage(
                "OPPONENT_DISCONNECTED",
                "Opponent disconnected. Waiting " + GRACE_PERIOD_SECONDS + "s..."
        ));

        // start the grace period countdown
        var timer = scheduler.schedule(
                () -> handleGracePeriodExpired(gameId, isWhite),
                GRACE_PERIOD_SECONDS,
                TimeUnit.SECONDS
        );
        session.setDisconnectTimer(timer);
    }

    private void handleGracePeriodExpired(UUID gameId, boolean wasWhiteWhoLeft) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) return;

        // if they somehow reconnected just before the timer fired, abort
        if (wasWhiteWhoLeft && session.isWhiteConnected()) return;
        if (!wasWhiteWhoLeft && session.isBlackConnected()) return;

        GameResult result = wasWhiteWhoLeft ? GameResult.BLACK_WINS : GameResult.WHITE_WINS;
        persistResult(gameId, result);
        broadcastEvent(gameId, new GameEventMessage("GAME_OVER", "Opponent abandoned. You win!"));
        endSession(gameId);
    }

    // --- move handling ---

    public GameStateMessage applyMove(UUID gameId, UUID userId, String from, String to, String promotion) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) throw new IllegalArgumentException("No active game with that id");

        Board board = session.getBoard();

        // turn enforcement — check the moving player owns the side to move
        boolean isWhiteTurn = board.getSideToMove() == Side.WHITE;
        boolean isWhitePlayer = session.isWhite(userId);
        boolean isBlackPlayer = session.isBlack(userId);

        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalStateException("You are not a player in this game");
        }
        if (isWhiteTurn && !isWhitePlayer) {
            throw new IllegalStateException("It is not your turn");
        }
        if (!isWhiteTurn && !isBlackPlayer) {
            throw new IllegalStateException("It is not your turn");
        }

        Square fromSq = Square.fromValue(from.toUpperCase());
        Square toSq = Square.fromValue(to.toUpperCase());

        Move move = new Move(fromSq, toSq);
        if (!board.legalMoves().contains(move)) throw new IllegalStateException("Illegal move");

        board.doMove(move);

        // the side that just moved is now the opposite of getSideToMove()
        // so if it's now BLACK's turn, WHITE just moved and won (on checkmate)
        Side sideJustMoved = board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE;
        GameStateMessage state = buildStateMessage(board, from + to);

        if (!state.status().equals("ONGOING") && !state.status().equals("CHECK")) {
            GameResult result = resolveResult(state.status(), sideJustMoved);
            persistResult(gameId, result);
            scheduler.schedule(() -> endSession(gameId), 5, TimeUnit.SECONDS);
        }

        return state;
    }

    public GameStateMessage getState(UUID gameId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) throw new IllegalArgumentException("No active game with that id");
        return buildStateMessage(session.getBoard(), null);
    }

    public void endSession(UUID gameId) {
        GameSession session = activeSessions.remove(gameId);
        if (session != null) {
            sessionToGame.remove(session.getWhiteSessionId());
            sessionToGame.remove(session.getBlackSessionId());
        }
    }

    // --- helpers ---

    private void broadcastEvent(UUID gameId, GameEventMessage event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/events", event);
    }

    @Transactional
    private void persistResult(UUID gameId, GameResult result) {
        gameRepository.findById(gameId).ifPresent(game -> {
            game.setResult(result);
            game.setEndedAt(LocalDateTime.now());
            gameRepository.save(game);
            // update both players' ratings - ABANDONED games are skipped inside EloService
            eloService.updateRatings(game.getWhite(), game.getBlack(), result);
        });
    }

    private GameResult resolveResult(String status, Side sideJustMoved) {
        return switch (status) {
            case "CHECKMATE" -> sideJustMoved == Side.WHITE ? GameResult.WHITE_WINS : GameResult.BLACK_WINS;
            case "STALEMATE", "DRAW" -> GameResult.DRAW;
            default -> GameResult.ABANDONED;
        };
    }

    private GameStateMessage buildStateMessage(Board board, String lastMove) {
        String status;
        if (board.isMated()) {
            status = "CHECKMATE";
        } else if (board.isStaleMate()) {
            status = "STALEMATE";
        } else if (board.isDraw()) {
            status = "DRAW";
        } else if (board.isKingAttacked()) {
            status = "CHECK";
        } else {
            status = "ONGOING";
        }

        String turn = board.getSideToMove() == Side.WHITE ? "WHITE" : "BLACK";
        return new GameStateMessage(board.getFen(), lastMove, turn, status);
    }
}