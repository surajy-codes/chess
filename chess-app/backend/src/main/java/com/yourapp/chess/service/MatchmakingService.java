package com.yourapp.chess.service;

import com.yourapp.chess.model.dto.MatchFoundMessage;
import com.yourapp.chess.model.entity.Game;
import com.yourapp.chess.model.entity.User;
import com.yourapp.chess.repository.GameRepository;
import com.yourapp.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private record WaitingPlayer(UUID clientId, String username) {}

    private final Queue<WaitingPlayer> queue = new ConcurrentLinkedQueue<>();

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public synchronized void joinQueue(UUID clientId, String username) {
        queue.add(new WaitingPlayer(clientId, username));
        tryMatch();
    }

    public synchronized void leaveQueue(UUID clientId) {
        queue.removeIf(p -> p.clientId().equals(clientId));
    }

    private void tryMatch() {
        if (queue.size() < 2) {
            return;
        }

        WaitingPlayer p1 = queue.poll();
        WaitingPlayer p2 = queue.poll();

        User white = findOrCreateUser(p1.username());
        User black = findOrCreateUser(p2.username());

        Game game = new Game(white, black);
        gameRepository.save(game);
        gameService.startSession(game.getId(), white.getId(), black.getId());

        notify(p1.clientId(), game.getId(), "WHITE", black.getUsername());
        notify(p2.clientId(), game.getId(), "BLACK", white.getUsername());
    }

    private void notify(UUID clientId, UUID gameId, String color, String opponentUsername) {
        messagingTemplate.convertAndSend(
                "/topic/matchmaking/" + clientId,
                new MatchFoundMessage(gameId, color, opponentUsername)
        );
    }

    private User findOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username)));
    }
}