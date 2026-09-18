package fr.games.square.api.dao;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Optional;
import java.util.stream.Stream;

public interface GameDao {
    Stream<Game> findAll();
    Optional<Game> findById(String gameId);
    Game upsert(Game game);
    void delete(String gameId);
}
