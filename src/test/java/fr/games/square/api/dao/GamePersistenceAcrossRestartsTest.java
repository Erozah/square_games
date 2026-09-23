package fr.games.square.api.dao;

import fr.games.square.api.ApiApplication;
import fr.games.square.api.plugin.TicTacToePlugin;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GamePersistenceAcrossRestartsTest {

    @Test
    void testGameSurvivesApplicationRestart() throws InvalidPositionException {
        String gameId;

        // 1er cycle de vie de l'application (démarrage -> écriture -> arrêt)
        try (ConfigurableApplicationContext context1 = SpringApplication.run(
                ApiApplication.class,
                "--spring.profiles.active=h2",
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:./data/gamesdb_restart_test;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
        )) {
            JpaGameDao dao1 = context1.getBean(JpaGameDao.class);
            TicTacToePlugin plugin1 = context1.getBean(TicTacToePlugin.class);

            Game game = plugin1.createGame(2, 3);
            gameId = game.getId().toString();
            dao1.upsert(game);

            // Jouer un coup
            Token token = game.getRemainingTokens().stream().filter(Token::canMove).findFirst().orElseThrow();
            token.moveTo(new CellPosition(0, 0));
            dao1.upsert(game);
        }

        // 2e cycle de vie de l'application (redémarrage -> lecture)
        try (ConfigurableApplicationContext context2 = SpringApplication.run(
                ApiApplication.class,
                "--spring.profiles.active=h2",
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:./data/gamesdb_restart_test;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
        )) {
            JpaGameDao dao2 = context2.getBean(JpaGameDao.class);

            Optional<Game> reloaded = dao2.findById(gameId);
            assertTrue(reloaded.isPresent(), "La partie doit être retrouvée après redémarrage");
            Game restoredGame = reloaded.get();
            assertEquals(gameId, restoredGame.getId().toString());
            assertTrue(restoredGame.getBoard().containsKey(new CellPosition(0, 0)), "Le coup joué doit être présent");
        }
    }
}
