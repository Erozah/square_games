package fr.games.square.api.dao;

import fr.games.square.api.plugin.ConnectFourPlugin;
import fr.games.square.api.plugin.TaquinPlugin;
import fr.games.square.api.plugin.TicTacToePlugin;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class JpaGameDaoTest {

    @Autowired
    private JpaGameDao jpaGameDao;

    @Autowired
    private TicTacToePlugin ticTacToePlugin;

    @Autowired
    private TaquinPlugin taquinPlugin;

    @Autowired
    private ConnectFourPlugin connectFourPlugin;

    @Test
    void testUpsertAndFindTicTacToe() throws InvalidPositionException {
        Game game = ticTacToePlugin.createGame(2, 3);
        jpaGameDao.upsert(game);

        Optional<Game> found = jpaGameDao.findById(game.getId().toString());
        assertTrue(found.isPresent());
        Game reloaded = found.get();
        assertEquals(game.getId(), reloaded.getId());
        assertEquals("tictactoe", reloaded.getFactoryId());
        assertEquals(3, reloaded.getBoardSize());

        // Play a move
        Token token = reloaded.getRemainingTokens().stream().filter(Token::canMove).findFirst().orElseThrow();
        token.moveTo(new CellPosition(0, 0));
        jpaGameDao.upsert(reloaded);

        Game afterMove = jpaGameDao.findById(game.getId().toString()).orElseThrow();
        assertTrue(afterMove.getBoard().containsKey(new CellPosition(0, 0)));

        // Delete
        jpaGameDao.delete(game.getId().toString());
        assertTrue(jpaGameDao.findById(game.getId().toString()).isEmpty());
    }

    @Test
    void testUpsertAndFindTaquin() throws InvalidPositionException {
        Game game = taquinPlugin.createGame(1, 4);
        jpaGameDao.upsert(game);

        Optional<Game> found = jpaGameDao.findById(game.getId().toString());
        assertTrue(found.isPresent());
        Game reloaded = found.get();
        assertEquals(game.getId(), reloaded.getId());
        assertEquals(4, reloaded.getBoardSize());
        assertEquals(15, reloaded.getBoard().size());

        // Play a move if possible
        Token movable = reloaded.getBoard().values().stream().filter(Token::canMove).findFirst().orElse(null);
        if (movable != null && !movable.getAllowedMoves().isEmpty()) {
            movable.moveTo(movable.getAllowedMoves().iterator().next());
            jpaGameDao.upsert(reloaded);
            Game afterMove = jpaGameDao.findById(game.getId().toString()).orElseThrow();
            assertNotNull(afterMove);
        }
    }

    @Test
    void testUpsertAndFindConnectFour() throws InvalidPositionException {
        Game game = connectFourPlugin.createGame(2, 7);
        jpaGameDao.upsert(game);

        Optional<Game> found = jpaGameDao.findById(game.getId().toString());
        assertTrue(found.isPresent());
        Game reloaded = found.get();
        assertEquals(game.getId(), reloaded.getId());
        assertEquals(7, reloaded.getBoardSize());

        // Play a move
        Token token = reloaded.getRemainingTokens().stream().filter(Token::canMove).findFirst().orElseThrow();
        token.moveTo(new CellPosition(3, -1));
        jpaGameDao.upsert(reloaded);

        Game afterMove = jpaGameDao.findById(game.getId().toString()).orElseThrow();
        assertEquals(1, afterMove.getBoard().size());
        assertTrue(afterMove.getBoard().keySet().stream().anyMatch(pos -> pos.x() == 3));
    }
}
