package fr.games.square.api.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    public String id;

    public String factoryId;

    public int boardSize;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            indexes = {@Index(name = "idx_game_players_player_id", columnList = "player_id")}
    )
    @Column(name = "player_id", nullable = false)
    public Set<UUID> playerIds = new LinkedHashSet<>();

    @Version
    @Column(name = "version", columnDefinition = "bigint default 0")
    public Long version = 0L;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    public List<GameTokenEntity> tokens = new ArrayList<>();

    public GameEntity() {
    }

    public GameEntity(String id, String factoryId, int boardSize, Set<UUID> playerIds, List<GameTokenEntity> tokens) {
        this.id = id;
        this.factoryId = factoryId;
        this.boardSize = boardSize;
        this.playerIds = playerIds != null ? playerIds : new LinkedHashSet<>();
        this.tokens = tokens != null ? tokens : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(String factoryId) {
        this.factoryId = factoryId;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public Set<UUID> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(Set<UUID> playerIds) {
        this.playerIds = playerIds != null ? playerIds : new LinkedHashSet<>();
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<GameTokenEntity> getTokens() {
        return tokens;
    }

    public void setTokens(List<GameTokenEntity> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
    }
}
