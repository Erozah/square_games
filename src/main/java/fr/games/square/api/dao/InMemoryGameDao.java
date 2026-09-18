package fr.games.square.api.dao;

import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class InMemoryGameDao implements GameDao {

    private final Map<String, Game> games = new HashMap<>();

    @Override
    public Stream<Game> findAll() {
        return games.values().stream();
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public Game upsert(Game game) {
        games.put(game.getId().toString(), game);
        return game;
    }

    @Override
    public void delete(String gameId) {
        games.remove(gameId);
    }
}
