package com.yourapp.chess.controller;

import com.yourapp.chess.model.dto.MatchmakingJoinRequest;
import com.yourapp.chess.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MatchmakingSocketController {

    private final MatchmakingService matchmakingService;

    @MessageMapping("/matchmaking/join")
    public void join(MatchmakingJoinRequest request) {
        matchmakingService.joinQueue(request.clientId(), request.username());
    }

    @MessageMapping("/matchmaking/leave")
    public void leave(MatchmakingJoinRequest request) {
        matchmakingService.leaveQueue(request.clientId());
    }
}