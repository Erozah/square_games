package fr.games.square.api.game;

import fr.games.square.api.dao.GameDao;
import fr.games.square.api.plugin.GamePlugin;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class GameServiceImpl implements GameService {

    private final GameDao gameDao;
    private final List<GamePlugin> gamePlugins;

    public GameServiceImpl(GameDao gameDao, List<GamePlugin> gamePlugins) {
        this.gameDao = gameDao;
        this.gamePlugins = gamePlugins;
    }

    @Override
    public Game createGame(UUID userId, GameCreationParams params) {
        if (params == null || params.getGameType() == null || params.getGameType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de jeu (gameType) est requis");
        }

        GamePlugin plugin = gamePlugins.stream()
                .filter(p -> p.supports(params.getGameType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de jeu non supporté : " + params.getGameType()));

        try {
            Set<UUID> playerIds = new LinkedHashSet<>();
            playerIds.add(userId);
            if (params.getOpponentIds() != null) {
                playerIds.addAll(params.getOpponentIds());
            }
            int expectedPlayers = (params.getPlayerCount() != null && params.getPlayerCount() > 0)
                    ? params.getPlayerCount()
                    : 2;
            while (playerIds.size() < expectedPlayers) {
                playerIds.add(UUID.randomUUID());
            }
            int boardSize = (params.getBoardSize() != null && params.getBoardSize() > 0)
                    ? params.getBoardSize()
                    : plugin.createGame(null, null).getBoardSize();
            Game game = plugin.getFactory().createGame(boardSize, playerIds);
            return gameDao.upsert(game);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètres invalides : " + e.getMessage());
        }
    }

    @Override
    public Game getGame(UUID gameId) {
        if (gameId == null) {
            return null;
        }
        return gameDao.findById(gameId.toString()).orElse(null);
    }

    @Override
    public Collection<CellPosition> getAllowedMoves(UUID gameId) {
        Game game = getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }

        Optional<Token> nextToken = game.getRemainingTokens().stream()
                .filter(Token::canMove)
                .findFirst();

        if (nextToken.isPresent()) {
            return nextToken.get().getAllowedMoves();
        }

        return game.getBoard().values().stream()
                .filter(Token::canMove)
                .map(Token::getPosition)
                .toList();
    }

    @Override
    public Game playMove(UUID userId, UUID gameId, MoveParams params) {
        Game game = getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }

        if (!userId.equals(game.getCurrentPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce n'est pas votre tour de jouer !");
        }
        CellPosition targetPos = new CellPosition(params.getX(), params.getY());

        Token boardToken = game.getBoard().get(targetPos);
        if (boardToken != null && boardToken.canMove()) {
            Set<CellPosition> allowed = boardToken.getAllowedMoves();
            if (!allowed.isEmpty()) {
                try {
                    boardToken.moveTo(allowed.iterator().next());
                    return gameDao.upsert(game);
                } catch (InvalidPositionException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coup invalide : " + e.getMessage());
                }
            }
        }

        Token token = game.getRemainingTokens().stream()
                .filter(Token::canMove)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun coup possible"));

        try {
            token.moveTo(targetPos);
            return gameDao.upsert(game);
        } catch (InvalidPositionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coup invalide : " + e.getMessage());
        }
    }

    @Override
    public List<Game> getGamesForUser(UUID userId) {
        return gameDao.findAll()
                .filter(game -> game.getPlayerIds().contains(userId))
                .toList();
    }
}
