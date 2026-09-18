package fr.games.square.api.game;

import fr.games.square.api.dao.GameDao;
import fr.games.square.api.dao.InMemoryGameDao;
import fr.games.square.api.plugin.ConnectFourPlugin;
import fr.games.square.api.plugin.TaquinPlugin;
import fr.games.square.api.plugin.TicTacToePlugin;
import fr.le_campus_numerique.square_games.engine.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceImplTest {

    private GameServiceImpl gameService;
    private GameDao gameDao;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        var tictactoe = new TicTacToePlugin(messageSource, 2, 3);
        var taquin = new TaquinPlugin(messageSource, 1, 4);
        var connectFour = new ConnectFourPlugin(messageSource, 2, 7);

        gameDao = new InMemoryGameDao();
        gameService = new GameServiceImpl(gameDao, List.of(tictactoe, taquin, connectFour));
    }

    @Test
    void createGame_TicTacToe_Success() {
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        Game game = gameService.createGame(params);

        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals("tictactoe", game.getFactoryId());
        assertEquals(3, game.getBoardSize());
        assertEquals(2, game.getPlayerIds().size());

        Game retrieved = gameService.getGame(game.getId());
        assertNotNull(retrieved);
        assertEquals(game.getId(), retrieved.getId());
    }

    @Test
    void createGame_WithDefaults_Success() {
        GameCreationParams params = new GameCreationParams("tictactoe", 0, 0);
        Game game = gameService.createGame(params);

        assertNotNull(game);
        assertEquals(3, game.getBoardSize());
        assertEquals(2, game.getPlayerIds().size());
    }

    @Test
    void createGame_Taquin_Success() {
        GameCreationParams params = new GameCreationParams("taquin", 1, 4);
        Game game = gameService.createGame(params);

        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals("15 puzzle", game.getFactoryId());
        assertEquals(4, game.getBoardSize());
        assertEquals(1, game.getPlayerIds().size());
    }

    @Test
    void createGame_ConnectFour_Success() {
        GameCreationParams params = new GameCreationParams("connect4", 2, 7);
        Game game = gameService.createGame(params);

        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals("connect4", game.getFactoryId());
        assertEquals(7, game.getBoardSize());
        assertEquals(2, game.getPlayerIds().size());
    }

    @Test
    void createGame_UnknownGameType_ThrowsBadRequest() {
        GameCreationParams params = new GameCreationParams("unknown_game", 2, 3);
        assertThrows(ResponseStatusException.class, () -> gameService.createGame(params));
    }

    @Test
    void createGame_InvalidPlayerCount_ThrowsBadRequest() {
        GameCreationParams params = new GameCreationParams("tictactoe", 4, 3);
        assertThrows(ResponseStatusException.class, () -> gameService.createGame(params));
    }
}
