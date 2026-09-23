package fr.games.square.api.plugin;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TaquinPlugin implements GamePlugin {

    private final GameFactory factory = new TaquinGameFactory();
    private final MessageSource messageSource;
    private final int defaultPlayerCount;
    private final int defaultBoardSize;

    public TaquinPlugin(
            MessageSource messageSource,
            @Value("${game.taquin.default-player-count:1}") int defaultPlayerCount,
            @Value("${game.taquin.default-board-size:4}") int defaultBoardSize
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
        return messageSource.getMessage("game.taquin.name", null, "15 Puzzle", locale);
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
        return normalized.equals("15 puzzle") || normalized.equals("taquin") || normalized.equals("15puzzle");
    }

    @Override
    public GameFactory getFactory() {
        return factory;
    }
}
