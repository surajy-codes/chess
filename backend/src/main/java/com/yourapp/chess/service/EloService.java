package com.yourapp.chess.service;

import com.yourapp.chess.model.entity.GameResult;
import com.yourapp.chess.model.entity.User;
import com.yourapp.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EloService {

    private static final int K = 32;

    private final UserRepository userRepository;

    public void updateRatings(User white, User black, GameResult result) {
        double whiteExpected = expectedScore(white.getRating(), black.getRating());
        double blackExpected = expectedScore(black.getRating(), white.getRating());

        double whiteActual;
        double blackActual;

        switch (result) {
            case WHITE_WINS -> { whiteActual = 1.0; blackActual = 0.0; }
            case BLACK_WINS -> { whiteActual = 0.0; blackActual = 1.0; }
            case DRAW       -> { whiteActual = 0.5; blackActual = 0.5; }
            default         -> { return; } // ABANDONED - no rating change
        }

        white.setRating(newRating(white.getRating(), whiteActual, whiteExpected));
        black.setRating(newRating(black.getRating(), blackActual, blackExpected));

        userRepository.save(white);
        userRepository.save(black);
    }

    private double expectedScore(int playerRating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - playerRating) / 400.0));
    }

    private int newRating(int currentRating, double actual, double expected) {
        return (int) Math.round(currentRating + K * (actual - expected));
    }
}