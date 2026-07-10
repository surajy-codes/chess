package com.yourapp.chess.controller;

import com.yourapp.chess.model.dto.CreateRoomRequest;
import com.yourapp.chess.model.dto.CreateRoomResponse;
import com.yourapp.chess.model.dto.GameStateMessage;
import com.yourapp.chess.model.dto.JoinRoomRequest;
import com.yourapp.chess.model.dto.JoinRoomResponse;
import com.yourapp.chess.service.GameService;
import com.yourapp.chess.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final RoomService roomService;
    private final GameService gameService;

    @PostMapping("/room")
    public CreateRoomResponse createRoom(@RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.username());
    }

    @PostMapping("/{roomId}/join")
    public JoinRoomResponse joinRoom(@PathVariable UUID roomId, @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(roomId, request.username());
    }

    @GetMapping("/{roomId}/state")
    public GameStateMessage getState(@PathVariable UUID roomId) {
        return gameService.getState(roomId);
    }
}
