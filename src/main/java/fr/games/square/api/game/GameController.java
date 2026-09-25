package fr.games.square.api.game;

import fr.games.square.api.client.UserValidationClient;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UUID userId,
            @RequestBody GameCreationParams params) {
        return gameService.createGame(userId, params);
    }

    @Operation(summary = "Lister les parties de l'utilisateur connecté")
    @GetMapping("/games")
    public List<Game> getGames(@AuthenticationPrincipal UUID userId) {
        return gameService.getGamesForUser(userId);
    }

    @Operation(summary = "Obtenir les détails d'une partie'")
    @GetMapping("/games/{gameId}")
    public Game getGame(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId
    ) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        return game;
    }

    @Operation(summary = "Obtenir la liste des coups jouables")
    @GetMapping("/games/{gameId}/moves")
    public Collection<CellPosition> getAllowedMoves(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId
    ) {
        return gameService.getAllowedMoves(gameId);
    }

    @Operation(summary = "Jouer un coup sur le plateau (vérifie le tour du joueur)")
    @PostMapping("/games/{gameId}/moves")
    public Game playMove(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId,
            @RequestBody MoveParams params) {
        return gameService.playMove(userId, gameId, params);
    }
}
