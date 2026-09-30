package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import fr.le_campus_numerique.square_games.engine.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
            existing.tokens.clear();
            if (newEntity.tokens != null) {
                existing.tokens.addAll(newEntity.tokens);
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
