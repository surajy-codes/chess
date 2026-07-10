package com.yourapp.chess.service;

import com.yourapp.chess.model.dto.CreateRoomResponse;
import com.yourapp.chess.model.dto.JoinRoomResponse;
import com.yourapp.chess.model.entity.Game;
import com.yourapp.chess.model.entity.User;
import com.yourapp.chess.repository.GameRepository;
import com.yourapp.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameService gameService;

    public CreateRoomResponse createRoom(String username) {
        User white = findOrCreateUser(username);

        Game game = new Game();
        game.setWhite(white);
        gameRepository.save(game);

        return new CreateRoomResponse(game.getId(), "WHITE");
    }

    public JoinRoomResponse joinRoom(UUID roomId, String username) {
        Game game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (game.getBlack() != null) {
            throw new IllegalStateException("Room is already full");
        }

        User black = findOrCreateUser(username);
        game.setBlack(black);
        gameRepository.save(game);

        gameService.startSession(game.getId(), game.getWhite().getId(), black.getId());

        return new JoinRoomResponse(game.getId(), "BLACK", game.getWhite().getUsername());
    }

    private User findOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username)));
    }
}