package fr.games.square.api.plugin;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConnectFourPlugin implements GamePlugin {

    private final GameFactory factory = new ConnectFourGameFactory();
    private final MessageSource messageSource;
    private final int defaultPlayerCount;
    private final int defaultBoardSize;

    public ConnectFourPlugin(
            MessageSource messageSource,
            @Value("${game.connectfour.default-player-count:2}") int defaultPlayerCount,
            @Value("${game.connectfour.default-board-size:7}") int defaultBoardSize
    ) {
        this.messageSource = messageSource;
        this.defaultPlayerCount = defaultPlayerCount;
        this.defaultBoardSize = defaultBoardSize;
    }

    @Override
    public String getId() {
        return factory.getGameFactoryId();
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.connectfour.name", null, "Connect Four", locale);
    }

    @Override
    public Game createGame(Integer playerCount, Integer boardSize) {
        int actualPlayerCount = (playerCount != null && playerCount > 0) ? playerCount : defaultPlayerCount;
        int actualBoardSize = (boardSize != null && boardSize > 0) ? boardSize : defaultBoardSize;
        return factory.createGame(actualPlayerCount, actualBoardSize);
    }

    @Override
    public boolean supports(String gameType) {
        if (gameType == null) {
            return false;
        }
        String normalized = gameType.trim().toLowerCase();
        return normalized.equals("connect4") || normalized.equals("connectfour") || normalized.equals("puissance4") || normalized.equals("connect-four");
    }

    @Override
    public GameFactory getFactory() {
        return factory;
    }
}
