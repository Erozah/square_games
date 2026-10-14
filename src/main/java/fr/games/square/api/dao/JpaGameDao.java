package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import fr.games.square.api.entity.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@Primary
@Profile("!inmemory & !jdbc")
@Transactional
public class JpaGameDao implements GameDao {

    private static final Logger log = LoggerFactory.getLogger(JpaGameDao.class);

    private final GameEntityRepository repository;
    private final GameMapper mapper;

    public JpaGameDao(GameEntityRepository repository, GameMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Game> findAll() {
        return repository.findAll().stream()
                .map(entity -> {
                    try {
                        return mapper.toGame(entity);
                    } catch (Throwable t) {
                        log.error("Failed to map GameEntity {} to Game: {}", entity.id, t.getMessage(), t);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull);
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Game> findByPlayerId(UUID userId) {
        if (userId == null) {
            return Stream.empty();
        }
        return repository.findByPlayerId(userId).stream()
                .map(entity -> {
                    try {
                        return mapper.toGame(entity);
                    } catch (Throwable t) {
                        log.error("Failed to map GameEntity {} to Game: {}", entity.id, t.getMessage(), t);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(String gameId) {
        return repository.findById(gameId)
                .flatMap(entity -> {
                    try {
                        return Optional.ofNullable(mapper.toGame(entity));
                    } catch (Throwable t) {
                        log.error("Failed to map GameEntity {} to Game: {}", entity.id, t.getMessage(), t);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Game upsert(Game game) {
        GameEntity newEntity = mapper.toEntity(game);
        Optional<GameEntity> existingOpt = repository.findById(newEntity.id);
        if (existingOpt.isPresent()) {
            GameEntity existing = existingOpt.get();
            existing.factoryId = newEntity.factoryId;
            existing.boardSize = newEntity.boardSize;
            existing.playerIds = newEntity.playerIds;

            // In-place differential update to prevent ID exhaustion and WAL churn
            List<GameTokenEntity> currentTokens = existing.tokens;
            List<GameTokenEntity> incomingTokens = newEntity.tokens != null ? newEntity.tokens : Collections.emptyList();

            int commonSize = Math.min(currentTokens.size(), incomingTokens.size());
            for (int i = 0; i < commonSize; i++) {
                GameTokenEntity cur = currentTokens.get(i);
                GameTokenEntity inc = incomingTokens.get(i);
                cur.ownerId = inc.ownerId;
                cur.name = inc.name;
                cur.removed = inc.removed;
                cur.x = inc.x;
                cur.y = inc.y;
            }

            if (incomingTokens.size() > currentTokens.size()) {
                for (int i = commonSize; i < incomingTokens.size(); i++) {
                    currentTokens.add(incomingTokens.get(i));
                }
            } else if (currentTokens.size() > incomingTokens.size()) {
                while (currentTokens.size() > incomingTokens.size()) {
                    currentTokens.remove(currentTokens.size() - 1);
                }
            }

            repository.save(existing);
        } else {
            repository.save(newEntity);
        }
        return game;
    }

    @Override
    public void delete(String gameId) {
        repository.deleteById(gameId);
    }
}
