package fr.games.square.api.game;

import fr.games.square.api.client.UserValidationClient;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Tag(name = "Jeux", description = "Gestion des parties et coups de plateau")
@RestController
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Créer une nouvelle partie (X-UserId requis)")
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public Game createGame(
            @RequestHeader("X-UserId") UUID userId,
            @RequestBody GameCreationParams params) {
        validateUser(userId);
        return gameService.createGame(userId, params);
    }

    @Operation(summary = "Lister les parties de l'utilisateur connecté")
    @GetMapping("/games")
    public List<Game> getGames(@RequestHeader("X-UserId") UUID userId) {
        validateUser(userId);
        return gameService.getGamesForUser(userId);
    }

    @Operation(summary = "Obtenir les détails d'une partie'")
    @GetMapping("/games/{gameId}")
    public Game getGame(
            @RequestHeader("X-UserId") UUID userId,
            @PathVariable UUID gameId
    ) {
        validateUser(userId);
        Game game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        return game;
    }

    @Operation(summary = "Obtenir la liste des coups jouables")
    @GetMapping("/games/{gameId}/moves")
    public Collection<CellPosition> getAllowedMoves(
            @RequestHeader("X-UserId") UUID userId,
            @PathVariable UUID gameId
    ) {
        validateUser(userId);
        return gameService.getAllowedMoves(gameId);
    }

    @Operation(summary = "Jouer un coup sur le plateau (vérifie le tour du joueur)")
    @PostMapping("/games/{gameId}/moves")
    public Game playMove(
            @RequestHeader("X-UserId") UUID userId,
            @PathVariable UUID gameId,
            @RequestBody MoveParams params) {
        validateUser(userId);
        return gameService.playMove(userId, gameId, params);
    }

    private void validateUser(UUID userId) {
        if (!userValidationClient.isValidUser(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non reconnu");
        }
    }
}
