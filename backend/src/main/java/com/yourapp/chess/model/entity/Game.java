package com.yourapp.chess.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_id", nullable = false)
    private User white;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_id")
    private User black;

    // null while the game is still in progress
    @Enumerated(EnumType.STRING)
    private GameResult result;

    @Column(columnDefinition = "TEXT")
    private String pgn;

    @CreationTimestamp
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    public Game(User white, User black) {
        this.white = white;
        this.black = black;
    }
}