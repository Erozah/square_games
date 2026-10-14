package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import fr.games.square.api.entity.GameTokenEntity;
import fr.games.square.api.plugin.GamePlugin;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.InconsistentGameDefinitionException;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameMapper {

    private final List<GamePlugin> gamePlugins;

    public GameMapper(List<GamePlugin> gamePlugins) {
        this.gamePlugins = gamePlugins;
    }

    public GameEntity toEntity(Game game) {
        GameEntity entity = new GameEntity();
        entity.id = game.getId().toString();
        entity.factoryId = game.getFactoryId();
        entity.boardSize = game.getBoardSize();
        entity.playerIds = new LinkedHashSet<>(game.getPlayerIds());

        GamePlugin plugin = findPlugin(game.getFactoryId());
        if (plugin != null) {
            entity.tokens = plugin.extractTokenEntities(game);
        } else {
            entity.tokens = Collections.emptyList();
        }

        return entity;
    }

    public Game toGame(GameEntity entity) {
        GamePlugin plugin = findPlugin(entity.factoryId);
        if (plugin == null) {
            throw new IllegalArgumentException("Unsupported game type: " + entity.factoryId);
        }

        GameFactory factory = plugin.getFactory();
        List<UUID> players = new ArrayList<>(entity.playerIds);

        List<TokenPosition<UUID>> boardTokens = plugin.reconstructBoardTokens(entity.tokens);
        List<TokenPosition<UUID>> removedTokens = plugin.reconstructRemovedTokens(entity.tokens);

        if (plugin.supports("tictactoe") && players.size() == 2 && boardTokens != null && !boardTokens.isEmpty()) {
            UUID p0 = players.get(0);
            UUID p1 = players.get(1);
            long count0 = boardTokens.stream().filter(t -> Objects.equals(p0, t.owner())).count();
            long count1 = boardTokens.stream().filter(t -> Objects.equals(p1, t.owner())).count();
            if (count0 > count1) {
                players.set(0, p1);
                players.set(1, p0);
            }
        }

        try {
            return factory.createGameWithIds(
                    UUID.fromString(entity.id),
                    entity.boardSize,
                    players,
                    boardTokens,
                    removedTokens
            );
        } catch (InconsistentGameDefinitionException e) {
            throw new IllegalStateException("Failed to reconstruct game from entity: " + e.getMessage(), e);
        }
    }

    private GamePlugin findPlugin(String factoryId) {
        if (factoryId == null) {
            return null;
        }
        return gamePlugins.stream()
                .filter(p -> p.supports(factoryId) || p.getId().equalsIgnoreCase(factoryId.trim()))
                .findFirst()
                .orElse(null);
    }
}
