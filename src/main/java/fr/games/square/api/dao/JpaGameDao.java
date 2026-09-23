package fr.games.square.api.dao;

import fr.games.square.api.dao.entity.GameEntity;
import fr.le_campus_numerique.square_games.engine.Game;
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

    private final GameEntityRepository repository;
    private final GameMapper mapper;

    public JpaGameDao(GameEntityRepository repository, GameMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Game> findAll() {
        return repository.findAll().stream().map(mapper::toGame);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(String gameId) {
        return repository.findById(gameId).map(mapper::toGame);
    }

    @Override
    public Game upsert(Game game) {
        GameEntity entity = mapper.toEntity(game);
        repository.save(entity);
        return game;
    }

    @Override
    public void delete(String gameId) {
        repository.deleteById(gameId);
    }
}
