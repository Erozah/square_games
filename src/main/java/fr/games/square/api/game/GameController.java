package fr.games.square.api.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.UUID;

@RestController
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public Game createGame(@RequestBody GameCreationParams params) {
        return gameService.createGame(params);
    }

    @GetMapping("/games/{gameId}")
    public Game getGame(@PathVariable UUID gameId) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        return game;
    }

    @GetMapping("/games/{gameId}/moves")
    public Collection<CellPosition> getAllowedMoves(@PathVariable UUID gameId) {
        return gameService.getAllowedMoves(gameId);
    }

    @PostMapping("/games/{gameId}/moves")
    public Game playMove(@PathVariable UUID gameId, @RequestBody MoveParams params) {
        return gameService.playMove(gameId, params);
    }
}
