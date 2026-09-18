package fr.games.square.api.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Collection;
import java.util.UUID;

public interface GameService {
    Game createGame(GameCreationParams params);
    Game getGame(UUID gameId);
    Collection<CellPosition> getAllowedMoves(UUID gameId);
    Game playMove(UUID gameId, MoveParams params);
}
