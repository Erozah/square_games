package fr.games.square.api.dto;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameStatus;

import java.util.*;

public record GameResponseDto(
        UUID id,
        String gameType,
        int boardSize,
        GameStatus status,
        UUID currentPlayerId,
        Set<UUID> playerIds,
        Map<String, TokenResponseDto> board
) {
    public static GameResponseDto fromDomain(Game game) {
        if (game == null) {
            return null;
        }
        Map<String, TokenResponseDto> boardMap = new LinkedHashMap<>();
        if (game.getBoard() != null) {
            game.getBoard().forEach((pos, token) -> {
                String key = pos.x() + "," + pos.y();
                boardMap.put(key, new TokenResponseDto(
                        token.getName(),
                        token.getOwnerId().orElse(null),
                        pos.x(),
                        pos.y(),
                        token.canMove()
                ));
            });
        }
        return new GameResponseDto(
                game.getId(),
                game.getFactoryId(),
                game.getBoardSize(),
                game.getStatus(),
                game.getCurrentPlayerId(),
                game.getPlayerIds(),
                boardMap
        );
    }
}
