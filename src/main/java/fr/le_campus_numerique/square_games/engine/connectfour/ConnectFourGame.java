package fr.le_campus_numerique.square_games.engine.connectfour;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;

/**
 * Patched replacement for the engine's ConnectFourGame to fix:
 * 1. Offset row indexing (shifting tokens to row 1 instead of 0)
 * 2. Duplicate key exceptions when adding subsequent tokens
 * 3. AssertionError when player turn switches
 * 4. Token owner inversion
 */
public class ConnectFourGame implements Game {

    private final fr.games.square.api.game.connectfour.ConnectFourGame delegate;

    public ConnectFourGame(@NotNull UUID redPlayerId, @NotNull UUID yellowPlayerId) {
        this.delegate = new fr.games.square.api.game.connectfour.ConnectFourGame(redPlayerId, yellowPlayerId);
    }

    ConnectFourGame(
            UUID id,
            List<@NotNull UUID> playerIds,
            @NotNull List<@NotNull List<Integer>> tokenOwnerIndexesByColumn) {

        List<TokenPosition<UUID>> boardTokens = new ArrayList<>();
        if (tokenOwnerIndexesByColumn != null && playerIds != null && playerIds.size() == 2) {
            for (int col = 0; col < tokenOwnerIndexesByColumn.size(); col++) {
                List<Integer> colTokens = tokenOwnerIndexesByColumn.get(col);
                if (colTokens != null) {
                    for (int row = 0; row < colTokens.size(); row++) {
                        int ownerIdx = colTokens.get(row);
                        UUID owner = (ownerIdx >= 0 && ownerIdx < playerIds.size()) ? playerIds.get(ownerIdx) : playerIds.get(0);
                        boardTokens.add(new TokenPosition<>(owner, "token", col, row));
                    }
                }
            }
        }

        try {
            this.delegate = new fr.games.square.api.game.connectfour.ConnectFourGame(id, playerIds, boardTokens);
        } catch (InconsistentGameDefinitionException e) {
            throw new IllegalStateException("Failed to construct ConnectFourGame: " + e.getMessage(), e);
        }
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }

    @Override
    public String getFactoryId() {
        return ConnectFourGameFactory.ID;
    }

    @Override
    public Set<UUID> getPlayerIds() {
        return delegate.getPlayerIds();
    }

    @Override
    public GameStatus getStatus() {
        return delegate.getStatus();
    }

    @Override
    public UUID getCurrentPlayerId() {
        return delegate.getCurrentPlayerId();
    }

    @Override
    public int getBoardSize() {
        return delegate.getBoardSize();
    }

    @Override
    public Map<CellPosition, Token> getBoard() {
        return delegate.getBoard();
    }

    @Override
    public Collection<Token> getRemainingTokens() {
        return delegate.getRemainingTokens();
    }

    @Override
    public Collection<Token> getRemovedTokens() {
        return delegate.getRemovedTokens();
    }

    public List<Token> getWinningLine() {
        return delegate.getWinningLine();
    }
}
