package com.yourapp.chess.controller;

import com.yourapp.chess.model.dto.GameStateMessage;
import com.yourapp.chess.model.dto.MoveRequest;
import com.yourapp.chess.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable UUID gameId, MoveRequest move) {
        GameStateMessage state = gameService.applyMove(gameId, move.from(), move.to(), move.promotion());
        messagingTemplate.convertAndSend("/topic/game/" + gameId, state);
    }

    @MessageExceptionHandler
    public void handleException(Exception ex) {
        System.err.println("Move error: " + ex.getMessage());
    }
}