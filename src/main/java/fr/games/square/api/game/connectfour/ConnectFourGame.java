package fr.games.square.api.game.connectfour;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;

public class ConnectFourGame implements Game {

    public static final int COLUMN_COUNT = 7;
    public static final int ROW_COUNT = 6;
    public static final int WIN_GOAL = 4;
    public static final String FACTORY_ID = "connect4";

    private final UUID id;
    private final UUID redPlayerId;
    private final UUID yellowPlayerId;

    private final Map<CellPosition, Token> board = new LinkedHashMap<>();
    private final Set<Token> remainingTokens = new LinkedHashSet<>();

    private boolean isYellowTurn;
    private List<Token> winningLine;

    public ConnectFourGame(@NotNull UUID redPlayerId, @NotNull UUID yellowPlayerId) {
        this(UUID.randomUUID(), redPlayerId, yellowPlayerId);
    }

    public ConnectFourGame(UUID id, @NotNull UUID redPlayerId, @NotNull UUID yellowPlayerId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.redPlayerId = Objects.requireNonNull(redPlayerId, "redPlayerId cannot be null");
        this.yellowPlayerId = Objects.requireNonNull(yellowPlayerId, "yellowPlayerId cannot be null");
        if (redPlayerId.equals(yellowPlayerId)) {
            throw new IllegalArgumentException("Player IDs must be different");
        }
        this.isYellowTurn = false;
        initRemainingTokens(0);
    }

    public ConnectFourGame(
            UUID id,
            @NotNull List<UUID> players,
            Collection<TokenPosition<UUID>> boardTokens) throws InconsistentGameDefinitionException {

        if (players == null || players.size() != 2) {
            throw new InconsistentGameDefinitionException("Connect Four requires exactly 2 players");
        }
        this.id = id != null ? id : UUID.randomUUID();

        UUID p0 = players.get(0);
        UUID p1 = players.get(1);

        int count0 = 0;
        int count1 = 0;
        if (boardTokens != null) {
            for (var tp : boardTokens) {
                if (p0.equals(tp.owner())) {
                    count0++;
                } else if (p1.equals(tp.owner())) {
                    count1++;
                } else {
                    throw new InconsistentGameDefinitionException("Unknown token owner: " + tp.owner());
                }
            }
        }

        // In Connect Four, Red always plays first.
        // Therefore, Red's token count must be either equal to Yellow's or 1 more.
        if (count1 > count0) {
            this.redPlayerId = p1;
            this.yellowPlayerId = p0;
            int tmp = count0;
            count0 = count1;
            count1 = tmp;
        } else {
            this.redPlayerId = p0;
            this.yellowPlayerId = p1;
        }

        if (count0 - count1 > 1) {
            throw new InconsistentGameDefinitionException("Invalid token distribution between players: " + count0 + " vs " + count1);
        }

        this.isYellowTurn = (count0 > count1);

        Map<Integer, List<TokenPosition<UUID>>> byCol = new HashMap<>();
        for (int c = 0; c < COLUMN_COUNT; c++) {
            byCol.put(c, new ArrayList<>());
        }

        if (boardTokens != null) {
            for (var tp : boardTokens) {
                if (tp.x() < 0 || tp.x() >= COLUMN_COUNT || tp.y() < 0 || tp.y() >= ROW_COUNT) {
                    throw new InconsistentGameDefinitionException("Invalid token coordinates: " + tp);
                }
                byCol.get(tp.x()).add(tp);
            }
        }

        for (int c = 0; c < COLUMN_COUNT; c++) {
            List<TokenPosition<UUID>> colTokens = byCol.get(c);
            colTokens.sort(Comparator.comparingInt(TokenPosition::y));
            for (int r = 0; r < colTokens.size(); r++) {
                TokenPosition<UUID> tp = colTokens.get(r);
                if (tp.y() != r) {
                    throw new InconsistentGameDefinitionException("Gap in column " + c + ": expected row " + r + " but got " + tp.y());
                }
                CellPosition pos = new CellPosition(c, r);
                String name = tp.owner().equals(this.redPlayerId) ? "R" : "Y";
                board.put(pos, new BoardToken(tp.owner(), name, pos));
            }
        }

        initRemainingTokens(board.size());
        updateWinningLine();
    }

    private void initRemainingTokens(int alreadyPlacedCount) {
        int totalCells = COLUMN_COUNT * ROW_COUNT;
        for (int i = alreadyPlacedCount; i < totalCells; i++) {
            UUID owner = (i % 2 == 0) ? redPlayerId : yellowPlayerId;
            String name = (i % 2 == 0) ? "R" : "Y";
            remainingTokens.add(new HandToken(owner, name));
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public Set<UUID> getPlayerIds() {
        LinkedHashSet<UUID> set = new LinkedHashSet<>(2);
        if (isYellowTurn) {
            set.add(yellowPlayerId);
            set.add(redPlayerId);
        } else {
            set.add(redPlayerId);
            set.add(yellowPlayerId);
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public GameStatus getStatus() {
        if (winningLine != null || remainingTokens.isEmpty()) {
            return GameStatus.TERMINATED;
        }
        return GameStatus.ONGOING;
    }

    @Override
    public UUID getCurrentPlayerId() {
        if (winningLine != null) {
            return winningLine.get(0).getOwnerId().orElse(null);
        }
        if (remainingTokens.isEmpty()) {
            return null; // draw
        }
        return isYellowTurn ? yellowPlayerId : redPlayerId;
    }

    @Override
    public int getBoardSize() {
        return COLUMN_COUNT;
    }

    @Override
    public Map<CellPosition, Token> getBoard() {
        return Collections.unmodifiableMap(board);
    }

    @Override
    public Collection<Token> getRemainingTokens() {
        return Collections.unmodifiableSet(remainingTokens);
    }

    @Override
    public Collection<Token> getRemovedTokens() {
        return Set.of();
    }

    public List<Token> getWinningLine() {
        return winningLine != null ? Collections.unmodifiableList(winningLine) : null;
    }

    private Set<CellPosition> getAvailableMoves() {
        if (getStatus() == GameStatus.TERMINATED) {
            return Set.of();
        }
        Set<CellPosition> moves = new HashSet<>();
        for (int col = 0; col < COLUMN_COUNT; col++) {
            int tokenCountInCol = 0;
            for (int row = 0; row < ROW_COUNT; row++) {
                if (board.containsKey(new CellPosition(col, row))) {
                    tokenCountInCol++;
                }
            }
            if (tokenCountInCol < ROW_COUNT) {
                moves.add(new CellPosition(col, -1));
                moves.add(new CellPosition(col, tokenCountInCol));
            }
        }
        return moves;
    }

    private CellPosition dropToken(HandToken token, CellPosition targetPos) throws InvalidPositionException {
        if (getStatus() == GameStatus.TERMINATED) {
            throw new InvalidPositionException("Game is already terminated");
        }
        int col = targetPos.x();
        if (col < 0 || col >= COLUMN_COUNT) {
            throw new InvalidPositionException("Invalid column: " + col);
        }

        int targetRow = -1;
        for (int row = 0; row < ROW_COUNT; row++) {
            if (!board.containsKey(new CellPosition(col, row))) {
                targetRow = row;
                break;
            }
        }

        if (targetRow == -1) {
            throw new InvalidPositionException("Column " + col + " is already full");
        }

        CellPosition finalPos = new CellPosition(col, targetRow);
        BoardToken placed = new BoardToken(token.ownerId, token.name, finalPos);
        board.put(finalPos, placed);
        remainingTokens.remove(token);

        updateWinningLine();
        isYellowTurn = !isYellowTurn;

        return finalPos;
    }

    private void updateWinningLine() {
        // Horizontal (-)
        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c <= COLUMN_COUNT - WIN_GOAL; c++) {
                List<Token> line = checkLine(c, r, 1, 0);
                if (line != null) {
                    winningLine = line;
                    return;
                }
            }
        }
        // Vertical (|)
        for (int c = 0; c < COLUMN_COUNT; c++) {
            for (int r = 0; r <= ROW_COUNT - WIN_GOAL; r++) {
                List<Token> line = checkLine(c, r, 0, 1);
                if (line != null) {
                    winningLine = line;
                    return;
                }
            }
        }
        // Diagonal (/)
        for (int c = 0; c <= COLUMN_COUNT - WIN_GOAL; c++) {
            for (int r = 0; r <= ROW_COUNT - WIN_GOAL; r++) {
                List<Token> line = checkLine(c, r, 1, 1);
                if (line != null) {
                    winningLine = line;
                    return;
                }
            }
        }
        // Diagonal (\)
        for (int c = 0; c <= COLUMN_COUNT - WIN_GOAL; c++) {
            for (int r = WIN_GOAL - 1; r < ROW_COUNT; r++) {
                List<Token> line = checkLine(c, r, 1, -1);
                if (line != null) {
                    winningLine = line;
                    return;
                }
            }
        }
    }

    private List<Token> checkLine(int startX, int startY, int dx, int dy) {
        List<Token> line = new ArrayList<>(WIN_GOAL);
        UUID owner = null;
        for (int step = 0; step < WIN_GOAL; step++) {
            CellPosition pos = new CellPosition(startX + step * dx, startY + step * dy);
            Token token = board.get(pos);
            if (token == null || token.getOwnerId().isEmpty()) {
                return null;
            }
            if (owner == null) {
                owner = token.getOwnerId().get();
            } else if (!owner.equals(token.getOwnerId().get())) {
                return null;
            }
            line.add(token);
        }
        return line;
    }

    private static class BoardToken implements Token {
        private final UUID ownerId;
        private final String name;
        private final CellPosition position;

        BoardToken(UUID ownerId, String name, CellPosition position) {
            this.ownerId = ownerId;
            this.name = name;
            this.position = position;
        }

        @Override public Optional<UUID> getOwnerId() { return Optional.of(ownerId); }
        @Override public String getName() { return name; }
        @Override public CellPosition getPosition() { return position; }
        @Override public boolean canMove() { return false; }
        @Override public Set<CellPosition> getAllowedMoves() { return Set.of(); }
        @Override public void moveTo(CellPosition position) throws InvalidPositionException {
            throw new InvalidPositionException("Placed token cannot move in Connect Four");
        }
        @Override public String toString() { return "[" + name + " at " + position + "]"; }
    }

    private class HandToken implements Token {
        private final UUID ownerId;
        private final String name;

        HandToken(UUID ownerId, String name) {
            this.ownerId = ownerId;
            this.name = name;
        }

        @Override public Optional<UUID> getOwnerId() { return Optional.of(ownerId); }
        @Override public String getName() { return name; }
        @Override public CellPosition getPosition() { return null; }

        @Override
        public boolean canMove() {
            return getStatus() == GameStatus.ONGOING && ownerId.equals(getCurrentPlayerId());
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            return canMove() ? getAvailableMoves() : Set.of();
        }

        @Override
        public void moveTo(CellPosition position) throws InvalidPositionException {
            if (!canMove()) {
                throw new InvalidPositionException("Not this player's turn");
            }
            dropToken(this, position);
        }
        @Override public String toString() { return "[" + name + " in hand]"; }
    }
}
