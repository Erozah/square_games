package fr.games.square.api.game;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public class GameCreationParams {

    @NotBlank(message = "Le type de jeu (gameType) est requis")
    private String gameType;

    @Min(value = 1, message = "Le nombre de joueurs doit être au moins 1")
    private Integer playerCount;

    @Min(value = 2, message = "La taille du plateau doit être au moins 2")
    private Integer boardSize;

    private List<UUID> opponentIds;

    public GameCreationParams() {
    }

    public GameCreationParams(String gameType, Integer playerCount, Integer boardSize) {
        this.gameType = gameType;
        this.playerCount = playerCount;
        this.boardSize = boardSize;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public Integer getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Integer playerCount) {
        this.playerCount = playerCount;
    }

    public Integer getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(Integer boardSize) {
        this.boardSize = boardSize;
    }

    public List<UUID> getOpponentIds() {
        return opponentIds;
    }

    public void setOpponentIds(List<UUID> opponentIds) {
        this.opponentIds = opponentIds;
    }
}
