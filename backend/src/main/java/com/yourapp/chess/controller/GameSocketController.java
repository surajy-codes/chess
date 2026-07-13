package com.yourapp.chess.controller;

import com.yourapp.chess.model.dto.GameStateMessage;
import com.yourapp.chess.model.dto.MoveRequest;
import com.yourapp.chess.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable UUID gameId,
                           MoveRequest move,
                           SimpMessageHeaderAccessor headerAccessor) {
        // userId comes from the JWT stored in session attributes on CONNECT - not from the client message
        UUID userId = (UUID) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) {
            throw new IllegalStateException("Not authenticated");
        }

        GameStateMessage state = gameService.applyMove(gameId, userId, move.from(), move.to(), move.promotion());
        messagingTemplate.convertAndSend("/topic/game/" + gameId, state);
    }

    @MessageMapping("/game/{gameId}/rejoin")
    public void handleRejoin(@DestinationVariable UUID gameId,
                             SimpMessageHeaderAccessor headerAccessor) {
        UUID userId = (UUID) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) throw new IllegalStateException("Not authenticated");

        String stompSessionId = headerAccessor.getSessionId();
        gameService.registerPlayerSession(gameId, userId, stompSessionId);

        GameStateMessage state = gameService.getState(gameId);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, state);
    }

    @MessageExceptionHandler
    public void handleException(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }
}