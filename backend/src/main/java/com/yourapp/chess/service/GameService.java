package com.yourapp.chess.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.yourapp.chess.model.GameSession;
import com.yourapp.chess.model.dto.GameStateMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    // gameId -> live in-memory board. Only active games live here;
    // finished games get persisted to the games table and removed from this map.
    private final Map<UUID, GameSession> activeSessions = new ConcurrentHashMap<>();

    public void startSession(UUID gameId, UUID whiteUserId, UUID blackUserId) {
        activeSessions.put(gameId, new GameSession(gameId, whiteUserId, blackUserId));
    }

    public GameStateMessage applyMove(UUID gameId, String from, String to, String promotion) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            throw new IllegalArgumentException("No active game with that id");
        }

        Board board = session.getBoard();
        Square fromSq = Square.fromValue(from.toUpperCase());
        Square toSq = Square.fromValue(to.toUpperCase());

        // TODO: handle promotion piece (pawn reaching the last rank) - chesslib's Move
        // class supports a promotion piece argument, we'll wire it in once you actually
        // hit a promotion case while testing
        Move move = new Move(fromSq, toSq);

        if (!board.legalMoves().contains(move)) {
            throw new IllegalStateException("Illegal move");
        }

        board.doMove(move);

        return buildStateMessage(board, from + to);
    }

    public GameStateMessage getState(UUID gameId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            throw new IllegalArgumentException("No active game with that id");
        }
        return buildStateMessage(session.getBoard(), null);
    }

    public void endSession(UUID gameId) {
        activeSessions.remove(gameId);
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