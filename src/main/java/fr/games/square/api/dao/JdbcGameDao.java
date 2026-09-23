package fr.games.square.api.dao;

import fr.games.square.api.dao.entity.GameEntity;
import fr.games.square.api.dao.entity.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
@Profile("jdbc")
@Transactional
public class JdbcGameDao implements GameDao {

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
    public Optional<Game> findById(String gameId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId);
        List<GameEntity> games = jdbcTemplate.query(
                "SELECT id, factory_id, board_size, player_ids FROM games WHERE id = :id",
                params,
                (rs, rowNum) -> {
                    GameEntity g = new GameEntity();
                    g.id = rs.getString("id");
                    g.factoryId = rs.getString("factory_id");
                    g.boardSize = rs.getInt("board_size");
                    g.playerIds = rs.getString("player_ids");
                    return g;
                }
        );

        if (games.isEmpty()) {
            return Optional.empty();
        }

        GameEntity entity = games.get(0);
        List<GameTokenEntity> tokens = jdbcTemplate.query(
                "SELECT id, owner_id, name, removed, x, y FROM game_tokens WHERE game_id = :id",
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
        return Optional.of(mapper.toGame(entity));
    }

    @Override
    public Game upsert(Game game) {
        GameEntity entity = mapper.toEntity(game);

        MapSqlParameterSource gameParams = new MapSqlParameterSource()
                .addValue("id", entity.id)
                .addValue("factoryId", entity.factoryId)
                .addValue("boardSize", entity.boardSize)
                .addValue("playerIds", entity.playerIds);

        int updated = jdbcTemplate.update(
                "UPDATE games SET factory_id = :factoryId, board_size = :boardSize, player_ids = :playerIds WHERE id = :id",
                gameParams
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO games (id, factory_id, board_size, player_ids) VALUES (:id, :factoryId, :boardSize, :playerIds)",
                    gameParams
            );
        }

        // Remove previous tokens for this game
        jdbcTemplate.update(
                "DELETE FROM game_tokens WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", entity.id)
        );

        // Insert new tokens
        if (entity.tokens != null && !entity.tokens.isEmpty()) {
            List<SqlParameterSource> tokenBatch = new ArrayList<>();
            for (GameTokenEntity t : entity.tokens) {
                tokenBatch.add(new MapSqlParameterSource()
                        .addValue("gameId", entity.id)
                        .addValue("ownerId", t.ownerId)
                        .addValue("name", t.name)
                        .addValue("removed", t.removed)
                        .addValue("x", t.x)
                        .addValue("y", t.y));
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO game_tokens (game_id, owner_id, name, removed, x, y) VALUES (:gameId, :ownerId, :name, :removed, :x, :y)",
                    tokenBatch.toArray(new SqlParameterSource[0])
            );
        }

        return game;
    }

    @Override
    public void delete(String gameId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId);
        jdbcTemplate.update("DELETE FROM game_tokens WHERE game_id = :id", params);
        jdbcTemplate.update("DELETE FROM games WHERE id = :id", params);
    }
}
