package fr.games.square.api.plugin;

import fr.games.square.api.entity.GameTokenEntity;
import fr.games.square.api.game.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ConnectFourPlugin implements GamePlugin {

    private final GameFactory factory = new ConnectFourGameFactory();
    private final MessageSource messageSource;
    private final int defaultPlayerCount;
    private final int defaultBoardSize;

    public ConnectFourPlugin(
            MessageSource messageSource,
            @Value("${game.connectfour.default-player-count:2}") int defaultPlayerCount,
            @Value("${game.connectfour.default-board-size:7}") int defaultBoardSize
    ) {
        this.messageSource = messageSource;
        this.defaultPlayerCount = defaultPlayerCount;
        this.defaultBoardSize = defaultBoardSize;
    }

    @Override
    public String getId() {
        return factory.getGameFactoryId();
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.connectfour.name", null, "Connect Four", locale);
    }

    @Override
    public Game createGame(Integer playerCount, Integer boardSize) {
        int actualPlayerCount = (playerCount != null && playerCount > 0) ? playerCount : defaultPlayerCount;
        int actualBoardSize = (boardSize != null && boardSize > 0) ? boardSize : defaultBoardSize;
        return factory.createGame(actualPlayerCount, actualBoardSize);
    }

    @Override
    public boolean supports(String gameType) {
        if (gameType == null) {
            return false;
        }
        String normalized = gameType.trim().toLowerCase();
        return normalized.equals("connect4") || normalized.equals("connectfour") || normalized.equals("puissance4") || normalized.equals("connect-four");
    }

    @Override
    public GameFactory getFactory() {
        return factory;
    }

    @Override
    public boolean isColumnBased() {
        return true;
    }

    @Override
    public CellPosition transformMove(Game game, CellPosition targetPos) {
        if (targetPos != null && targetPos.y() >= 0) {
            return new CellPosition(targetPos.x(), -1);
        }
        return targetPos;
    }

    @Override
    public List<ColumnAction> buildColumnActions(Game game, boolean isMyTurn, Collection<CellPosition> allowedPositions) {
        int numCols = 7;
        List<ColumnAction> actions = new ArrayList<>(numCols);
        for (int col = 0; col < numCols; col++) {
            boolean allowed = isMyTurn && allowedPositions != null && allowedPositions.contains(new CellPosition(col, -1));
            actions.add(new ColumnAction(col, allowed));
        }
        return actions;
    }

    @Override
    public List<List<CellView>> buildGridView(Game game, Collection<CellPosition> allowedPositions) {
        int numCols = 7;
        int numRows = 6;
        List<List<CellView>> grid = new ArrayList<>(numRows);

        Map<Integer, List<Token>> tokensByCol = game.getBoard().values().stream()
                .filter(t -> t.getOwnerId().isPresent() && t.getPosition() != null)
                .collect(Collectors.groupingBy(t -> t.getPosition().x()));

        for (int slot = numRows - 1; slot >= 0; slot--) {
            List<CellView> row = new ArrayList<>(numCols);
            for (int col = 0; col < numCols; col++) {
                List<Token> colTokens = tokensByCol.get(col);
                String tokenName = null;
                if (colTokens != null && !colTokens.isEmpty()) {
                    List<Token> sorted = colTokens.stream()
                            .sorted(Comparator.comparingInt(t -> t.getPosition().y()))
                            .toList();
                    if (slot < sorted.size()) {
                        tokenName = sorted.get(slot).getName();
                    }
                }
                row.add(new CellView(col, slot, tokenName, false));
            }
            grid.add(row);
        }
        return grid;
    }

    @Override
    public List<GameTokenEntity> extractTokenEntities(Game game) {
        List<GameTokenEntity> tokenEntities = new ArrayList<>();
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

    @Override
    public List<TokenPosition<UUID>> reconstructBoardTokens(List<GameTokenEntity> tokens) {
        if (tokens == null) {
            return Collections.emptyList();
        }
        List<TokenPosition<UUID>> boardTokens = new ArrayList<>();
        Map<Integer, List<GameTokenEntity>> byCol = tokens.stream()
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
        return boardTokens;
    }
}
