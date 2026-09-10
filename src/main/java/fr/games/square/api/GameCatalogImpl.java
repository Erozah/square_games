package fr.games.square.api;

import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class GameCatalogImpl implements GameCatalog {
    private final GameFactory ticTacToeGameFactory = new TicTacToeGameFactory();

    @Override
    public Collection<String> get() {
        return List.of(ticTacToeGameFactory.getGameFactoryId());
    }
}
