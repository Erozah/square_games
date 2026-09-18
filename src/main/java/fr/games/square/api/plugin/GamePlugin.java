package fr.games.square.api.plugin;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Locale;

public interface GamePlugin {

    String getId();

    String getName(Locale locale);

    Game createGame(Integer playerCount, Integer boardSize);

    default boolean supports(String gameType) {
        return gameType != null && getId().equalsIgnoreCase(gameType.trim());
    }
}
