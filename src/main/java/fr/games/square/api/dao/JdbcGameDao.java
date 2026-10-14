package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import fr.games.square.api.entity.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Repository
@Profile("jdbc")
@Transactional
public class JdbcGameDao implements GameDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcGameDao.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final GameMapper mapper;

    public JdbcGameDao(NamedParameterJdbcTemplate jdbcTemplate, GameMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Game> findAll() {
        List<String> ids = jdbcTemplate.query(
                "SELECT id FROM games",
                (rs, rowNum) -> rs.getString("id")
        );
        return ids.stream()
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Game> findByPlayerId(UUID userId) {
        if (userId == null) {
            return Stream.empty();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<String> ids = jdbcTemplate.query(
                "SELECT DISTINCT game_id FROM game_players WHERE player_id = :userId",
                params,
                (rs, rowNum) -> rs.getString("game_id")
        );
        return ids.stream()
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(String gameId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId);
        List<GameEntity> games = jdbcTemplate.query(
                "SELECT id, factory_id, board_size FROM games WHERE id = :id",
                params,
                (rs, rowNum) -> {
                    GameEntity g = new GameEntity();
                    g.id = rs.getString("id");
                    g.factoryId = rs.getString("factory_id");
                    g.boardSize = rs.getInt("board_size");
                    return g;
                }
        );

        if (games.isEmpty()) {
            return Optional.empty();
        }

        GameEntity entity = games.get(0);

        List<UUID> playerIds = jdbcTemplate.query(
                "SELECT player_id FROM game_players WHERE game_id = :id",
                params,
                (rs, rowNum) -> rs.getObject("player_id", UUID.class)
        );
        entity.playerIds = new LinkedHashSet<>(playerIds);

        List<GameTokenEntity> tokens = jdbcTemplate.query(
                "SELECT id, owner_id, name, removed, x, y FROM game_tokens WHERE game_id = :id ORDER BY id ASC",
                params,
                (rs, rowNum) -> {
                    GameTokenEntity te = new GameTokenEntity();
                    te.id = rs.getLong("id");
                    te.ownerId = rs.getString("owner_id");
                    te.name = rs.getString("name");
                    te.removed = rs.getBoolean("removed");
                    int x = rs.getInt("x");
                    te.x = rs.wasNull() ? null : x;
                    int y = rs.getInt("y");
                    te.y = rs.wasNull() ? null : y;
                    return te;
                }
        );

        entity.tokens = tokens;
        try {
            return Optional.ofNullable(mapper.toGame(entity));
        } catch (Throwable t) {
            log.error("Failed to map GameEntity {} to Game: {}", entity.id, t.getMessage(), t);
            return Optional.empty();
        }
    }

    @Override
    public Game upsert(Game game) {
        GameEntity entity = mapper.toEntity(game);

        // 1. Upsert game entity
        MapSqlParameterSource gameParams = new MapSqlParameterSource()
                .addValue("id", entity.id)
                .addValue("factoryId", entity.factoryId)
                .addValue("boardSize", entity.boardSize);

        int updated = jdbcTemplate.update(
                "UPDATE games SET factory_id = :factoryId, board_size = :boardSize WHERE id = :id",
                gameParams
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO games (id, factory_id, board_size) VALUES (:id, :factoryId, :boardSize)",
                    gameParams
            );
        }

        // 2. Refresh players association table
        jdbcTemplate.update(
                "DELETE FROM game_players WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", entity.id)
        );
        if (entity.playerIds != null && !entity.playerIds.isEmpty()) {
            List<SqlParameterSource> playerBatch = new ArrayList<>();
            for (UUID pid : entity.playerIds) {
                playerBatch.add(new MapSqlParameterSource()
                        .addValue("gameId", entity.id)
                        .addValue("playerId", pid));
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO game_players (game_id, player_id) VALUES (:gameId, :playerId)",
                    playerBatch.toArray(new SqlParameterSource[0])
            );
        }

        // 3. Differential token update (in-place modification without deleting all tokens)
        List<GameTokenEntity> existingTokens = jdbcTemplate.query(
                "SELECT id, owner_id, name, removed, x, y FROM game_tokens WHERE game_id = :gameId ORDER BY id ASC",
                new MapSqlParameterSource("gameId", entity.id),
                (rs, rowNum) -> {
                    GameTokenEntity te = new GameTokenEntity();
                    te.id = rs.getLong("id");
                    te.ownerId = rs.getString("owner_id");
                    te.name = rs.getString("name");
                    te.removed = rs.getBoolean("removed");
                    int x = rs.getInt("x");
                    te.x = rs.wasNull() ? null : x;
                    int y = rs.getInt("y");
                    te.y = rs.wasNull() ? null : y;
                    return te;
                }
        );

        List<GameTokenEntity> newTokens = entity.tokens != null ? entity.tokens : Collections.emptyList();
        int commonSize = Math.min(existingTokens.size(), newTokens.size());

        List<SqlParameterSource> updateBatch = new ArrayList<>();
        for (int i = 0; i < commonSize; i++) {
            GameTokenEntity oldToken = existingTokens.get(i);
            GameTokenEntity token = newTokens.get(i);

            boolean changed = !Objects.equals(oldToken.ownerId, token.ownerId)
                    || !Objects.equals(oldToken.name, token.name)
                    || oldToken.removed != token.removed
                    || !Objects.equals(oldToken.x, token.x)
                    || !Objects.equals(oldToken.y, token.y);

            if (changed) {
                updateBatch.add(new MapSqlParameterSource()
                        .addValue("id", oldToken.id)
                        .addValue("ownerId", token.ownerId)
                        .addValue("name", token.name)
                        .addValue("removed", token.removed)
                        .addValue("x", token.x)
                        .addValue("y", token.y));
            }
        }
        if (!updateBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "UPDATE game_tokens SET owner_id = :ownerId, name = :name, removed = :removed, x = :x, y = :y WHERE id = :id",
                    updateBatch.toArray(new SqlParameterSource[0])
            );
        }

        if (newTokens.size() > existingTokens.size()) {
            List<SqlParameterSource> insertBatch = new ArrayList<>();
            for (int i = commonSize; i < newTokens.size(); i++) {
                GameTokenEntity t = newTokens.get(i);
                insertBatch.add(new MapSqlParameterSource()
                        .addValue("gameId", entity.id)
                        .addValue("ownerId", t.ownerId)
                        .addValue("name", t.name)
                        .addValue("removed", t.removed)
                        .addValue("x", t.x)
                        .addValue("y", t.y));
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO game_tokens (game_id, owner_id, name, removed, x, y) VALUES (:gameId, :ownerId, :name, :removed, :x, :y)",
                    insertBatch.toArray(new SqlParameterSource[0])
            );
        } else if (existingTokens.size() > newTokens.size()) {
            List<SqlParameterSource> deleteBatch = new ArrayList<>();
            for (int i = commonSize; i < existingTokens.size(); i++) {
                deleteBatch.add(new MapSqlParameterSource("id", existingTokens.get(i).id));
            }
            jdbcTemplate.batchUpdate(
                    "DELETE FROM game_tokens WHERE id = :id",
                    deleteBatch.toArray(new SqlParameterSource[0])
            );
        }

        return game;
    }

    @Override
    public void delete(String gameId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId);
        jdbcTemplate.update("DELETE FROM game_tokens WHERE game_id = :id", params);
        jdbcTemplate.update("DELETE FROM game_players WHERE game_id = :id", params);
        jdbcTemplate.update("DELETE FROM games WHERE id = :id", params);
    }
}
