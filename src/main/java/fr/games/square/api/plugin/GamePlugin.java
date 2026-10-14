package fr.games.square.api.plugin;

import fr.games.square.api.entity.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;

import java.util.*;

public interface GamePlugin {

    String getId();

    String getName(Locale locale);

    Game createGame(Integer playerCount, Integer boardSize);

    default boolean supports(String gameType) {
        return gameType != null && getId().equalsIgnoreCase(gameType.trim());
    }

    GameFactory getFactory();

    default CellPosition transformMove(Game game, CellPosition targetPos) {
        return targetPos;
    }

    default boolean isColumnBased() {
        return false;
    }

    default List<ColumnAction> buildColumnActions(Game game, boolean isMyTurn, Collection<CellPosition> allowedPositions) {
        return Collections.emptyList();
    }

    default List<List<CellView>> buildGridView(Game game, Collection<CellPosition> allowedPositions) {
        int size = game.getBoardSize();
        List<List<CellView>> grid = new ArrayList<>(size);
        for (int x = 0; x < size; x++) {
            List<CellView> row = new ArrayList<>(size);
            for (int y = 0; y < size; y++) {
                CellPosition pos = new CellPosition(x, y);
                Token token = game.getBoard().get(pos);
                String tokenName = token != null ? token.getName() : null;
                boolean isAllowed = allowedPositions != null && allowedPositions.contains(pos);
                row.add(new CellView(x, y, tokenName, isAllowed));
            }
            grid.add(row);
        }
        return grid;
    }

    default List<GameTokenEntity> extractTokenEntities(Game game) {
        List<GameTokenEntity> tokenEntities = new ArrayList<>();
        game.getBoard().forEach((pos, token) -> {
            tokenEntities.add(new GameTokenEntity(
                    token.getOwnerId().map(UUID::toString).orElse(null),
                    token.getName(),
                    false,
                    pos.x(),
                    pos.y()
            ));
        });
        for (Token token : game.getRemainingTokens()) {
            tokenEntities.add(new GameTokenEntity(
                    token.getOwnerId().map(UUID::toString).orElse(null),
                    token.getName(),
                    false,
                    null,
                    null
            ));
        }
        for (Token token : game.getRemovedTokens()) {
            tokenEntities.add(new GameTokenEntity(
                    token.getOwnerId().map(UUID::toString).orElse(null),
                    token.getName(),
                    true,
                    token.getPosition() != null ? token.getPosition().x() : null,
                    token.getPosition() != null ? token.getPosition().y() : null
            ));
        }
        return tokenEntities;
    }

    default List<TokenPosition<UUID>> reconstructBoardTokens(List<GameTokenEntity> tokens) {
        if (tokens == null) {
            return Collections.emptyList();
        }
        List<TokenPosition<UUID>> list = new ArrayList<>();
        for (GameTokenEntity t : tokens) {
            if (!t.removed && t.x != null && t.y != null) {
                UUID ownerUuid = (t.ownerId != null && !t.ownerId.isBlank()) ? UUID.fromString(t.ownerId) : null;
                list.add(new TokenPosition<>(ownerUuid, t.name, t.x, t.y));
            }
        }
        return list;
    }

    default List<TokenPosition<UUID>> reconstructRemovedTokens(List<GameTokenEntity> tokens) {
        if (tokens == null) {
            return Collections.emptyList();
        }
        List<TokenPosition<UUID>> list = new ArrayList<>();
        for (GameTokenEntity t : tokens) {
            if (t.removed) {
                UUID ownerUuid = (t.ownerId != null && !t.ownerId.isBlank()) ? UUID.fromString(t.ownerId) : null;
                list.add(new TokenPosition<>(ownerUuid, t.name, t.x != null ? t.x : 0, t.y != null ? t.y : 0));
            }
        }
        return list;
    }
}
