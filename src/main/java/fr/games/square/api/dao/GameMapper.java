package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import fr.games.square.api.entity.GameTokenEntity;
import fr.games.square.api.plugin.GamePlugin;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.InconsistentGameDefinitionException;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
        entity.playerIds = game.getPlayerIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        List<GameTokenEntity> tokenEntities = new ArrayList<>();

        if (game.getFactoryId() != null && game.getFactoryId().toLowerCase().contains("connect")) {
            // Pour Puissance 4, normalisation des jetons par colonne de 0 à size-1
            Map<Integer, List<Token>> tokensByColumn = game.getBoard().values().stream()
                    .filter(t -> t.getOwnerId().isPresent() && t.getPosition() != null)
                    .collect(Collectors.groupingBy(t -> t.getPosition().x()));

            tokensByColumn.forEach((col, tokens) -> {
                List<Token> sorted = tokens.stream()
                        .sorted(Comparator.comparingInt(t -> t.getPosition().y()))
                        .toList();
                for (int row = 0; row < sorted.size(); row++) {
                    Token token = sorted.get(row);
                    tokenEntities.add(new GameTokenEntity(
                            token.getOwnerId().map(UUID::toString).orElse(null),
                            token.getName(),
                            false,
                            col,
                            row
                    ));
                }
            });
        } else {
            // Board tokens pour Morpion et Taquin
            game.getBoard().forEach((pos, token) -> {
                tokenEntities.add(new GameTokenEntity(
                        token.getOwnerId().map(UUID::toString).orElse(null),
                        token.getName(),
                        false,
                        pos.x(),
                        pos.y()
                ));
            });
        }

        // Remaining tokens (in hand, not on board yet)
        for (var token : game.getRemainingTokens()) {
            tokenEntities.add(new GameTokenEntity(
                    token.getOwnerId().map(UUID::toString).orElse(null),
                    token.getName(),
                    false,
                    null,
                    null
            ));
        }

        // Removed tokens
        for (var token : game.getRemovedTokens()) {
            tokenEntities.add(new GameTokenEntity(
                    token.getOwnerId().map(UUID::toString).orElse(null),
                    token.getName(),
                    true,
                    token.getPosition() != null ? token.getPosition().x() : null,
                    token.getPosition() != null ? token.getPosition().y() : null
            ));
        }

        entity.tokens = tokenEntities;
        return entity;
    }

    public Game toGame(GameEntity entity) {
        GamePlugin plugin = gamePlugins.stream()
                .filter(p -> p.supports(entity.factoryId) || p.getId().equalsIgnoreCase(entity.factoryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported game type: " + entity.factoryId));

        GameFactory factory = plugin.getFactory();

        List<UUID> players = Arrays.stream(entity.playerIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();

        List<TokenPosition<UUID>> boardTokens = new ArrayList<>();
        List<TokenPosition<UUID>> removedTokens = new ArrayList<>();

        if (entity.tokens != null) {
            if (entity.factoryId != null && entity.factoryId.toLowerCase().contains("connect")) {
                Map<Integer, List<GameTokenEntity>> byCol = entity.tokens.stream()
                        .filter(t -> !t.removed && t.x != null && t.y != null)
                        .collect(Collectors.groupingBy(t -> t.x));

                byCol.forEach((col, list) -> {
                    List<GameTokenEntity> sorted = list.stream()
                            .sorted(Comparator.comparingInt(t -> t.y != null ? t.y : 0))
                            .toList();
                    for (int row = 0; row < sorted.size(); row++) {
                        GameTokenEntity t = sorted.get(row);
                        UUID ownerUuid = (t.ownerId != null && !t.ownerId.isBlank()) ? UUID.fromString(t.ownerId) : null;
                        boardTokens.add(new TokenPosition<>(ownerUuid, t.name, col, row));
                    }
                });
            } else {
                for (GameTokenEntity t : entity.tokens) {
                    UUID ownerUuid = (t.ownerId != null && !t.ownerId.isBlank()) ? UUID.fromString(t.ownerId) : null;
                    if (t.removed) {
                        removedTokens.add(new TokenPosition<>(ownerUuid, t.name, t.x != null ? t.x : 0, t.y != null ? t.y : 0));
                    } else if (t.x != null && t.y != null) {
                        boardTokens.add(new TokenPosition<>(ownerUuid, t.name, t.x, t.y));
                    }
                }
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
}
