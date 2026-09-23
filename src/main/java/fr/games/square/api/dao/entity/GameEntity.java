package fr.games.square.api.dao.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    public String id;

    public String factoryId;

    public int boardSize;

    public String playerIds;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    public List<GameTokenEntity> tokens = new ArrayList<>();

    public GameEntity() {
    }

    public GameEntity(String id, String factoryId, int boardSize, String playerIds, List<GameTokenEntity> tokens) {
        this.id = id;
        this.factoryId = factoryId;
        this.boardSize = boardSize;
        this.playerIds = playerIds;
        this.tokens = tokens != null ? tokens : new ArrayList<>();
    }
}
