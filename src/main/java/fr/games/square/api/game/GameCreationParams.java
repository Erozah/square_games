package fr.games.square.api.game;

import java.util.List;
import java.util.UUID;


public class GameCreationParams {
    private String gameType;
    private Integer playerCount;
    private Integer boardSize;
    private List<UUID> opponentIds;

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

    public GameCreationParams() {
    }
}
