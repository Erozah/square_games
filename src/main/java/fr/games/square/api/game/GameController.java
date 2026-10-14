package fr.games.square.api.game;

import fr.games.square.api.dto.GameResponseDto;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(summary = "Créer une nouvelle partie")
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponseDto createGame(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GameCreationParams params) {
        Game game = gameService.createGame(userId, params);
        return GameResponseDto.fromDomain(game);
    }

    @Operation(summary = "Lister les parties de l'utilisateur connecté")
    @GetMapping("/games")
    public List<GameResponseDto> getGames(@AuthenticationPrincipal UUID userId) {
        return gameService.getGamesForUser(userId).stream()
                .map(GameResponseDto::fromDomain)
                .toList();
    }

    @Operation(summary = "Obtenir les détails d'une partie")
    @GetMapping("/games/{gameId}")
    public GameResponseDto getGame(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId
    ) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        if (userId != null && !game.getPlayerIds().contains(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous ne participez pas à cette partie");
        }
        return GameResponseDto.fromDomain(game);
    }

    @Operation(summary = "Obtenir la liste des coups jouables")
    @GetMapping("/games/{gameId}/moves")
    public Collection<CellPosition> getAllowedMoves(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId
    ) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        if (userId != null && !game.getPlayerIds().contains(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous ne participez pas à cette partie");
        }
        return gameService.getAllowedMoves(gameId);
    }

    @Operation(summary = "Jouer un coup sur le plateau (vérifie le tour du joueur)")
    @PostMapping("/games/{gameId}/moves")
    public GameResponseDto playMove(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameId,
            @Valid @RequestBody MoveParams params) {
        Game game = gameService.playMove(userId, gameId, params);
        return GameResponseDto.fromDomain(game);
    }
}
