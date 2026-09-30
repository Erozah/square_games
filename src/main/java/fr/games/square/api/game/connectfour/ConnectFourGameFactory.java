package fr.games.square.api.game.connectfour;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.*;

public class ConnectFourGameFactory implements GameFactory {

    public static final String ID = "connect4";

    @Override
    public String getGameFactoryId() {
        return ID;
    }

    @Override
    public IntRange getPlayerCountRange() {
        return new IntRange(2);
    }

    @Override
    public IntRange getBoardSizeRange(@Positive int playerCount) {
        if (playerCount != 2) {
            throw new IllegalArgumentException("playerCount must be equal to 2");
        }
        return new IntRange(7);
    }

    @Override
    public Game createGame(int playerCount, int boardSize) {
        if (playerCount != 2) {
            throw new IllegalArgumentException("playerCount must be equal to 2");
        }
        if (boardSize != 7) {
            throw new IllegalArgumentException("boardSize must be equal to 7");
        }
        return new ConnectFourGame(UUID.randomUUID(), UUID.randomUUID());
    }

    @Override
    public Game createGame(int boardSize, @NotNull Set<UUID> playerIds) {
        if (boardSize != 7) {
            throw new IllegalArgumentException("boardSize must be equal to 7");
        }
        if (playerIds == null || playerIds.size() != 2) {
            throw new IllegalArgumentException("playerIds must contain exactly 2 players");
        }
        Iterator<UUID> it = playerIds.iterator();
        return new ConnectFourGame(it.next(), it.next());
    }

    @Override
    public <K> Game createGame(
            int boardSize,
            @NotEmpty List<K> players,
            @NotNull Collection<TokenPosition<K>> boardTokens,
            @NotNull Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        if (players == null || players.size() != 2) {
            throw new InconsistentGameDefinitionException("Connect Four requires exactly 2 players");
        }
        List<UUID> playerUuids = players.stream()
                .map(p -> UUID.fromString(p.toString()))
                .toList();
        List<TokenPosition<UUID>> converted = (boardTokens != null)
                ? boardTokens.stream()
                .map(tp -> new TokenPosition<>(UUID.fromString(tp.owner().toString()), tp.tokenName(), tp.x(), tp.y()))
                .toList()
                : Collections.emptyList();
        return new ConnectFourGame(UUID.randomUUID(), playerUuids, converted);
    }

    @Override
    public Game createGameWithIds(
            UUID gameId,
            int boardSize,
            @NotEmpty List<UUID> players,
            @NotNull Collection<TokenPosition<UUID>> boardTokens,
            @NotNull Collection<TokenPosition<UUID>> removedTokens) throws InconsistentGameDefinitionException {
        if (boardSize != 7) {
            throw new InconsistentGameDefinitionException("boardSize must be equal to 7");
        }
        return new ConnectFourGame(gameId, players, boardTokens);
    }
}
