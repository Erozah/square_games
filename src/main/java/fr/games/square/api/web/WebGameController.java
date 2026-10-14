package fr.games.square.api.web;

import fr.games.square.api.catalog.GameCatalog;
import fr.games.square.api.catalog.GameCatalogItem;
import fr.games.square.api.client.UserValidationClient;
import fr.games.square.api.game.GameCreationParams;
import fr.games.square.api.game.GameService;
import fr.games.square.api.game.GameServiceImpl;
import fr.games.square.api.game.MoveParams;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.Token;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

import fr.games.square.api.plugin.CellView;
import fr.games.square.api.plugin.ColumnAction;
import fr.games.square.api.plugin.GamePlugin;

@Controller
@RequestMapping("/web")
public class WebGameController {

    private final GameService gameService;
    private final GameCatalog gameCatalog;
    private final UserValidationClient userValidationClient;
    private final List<GamePlugin> gamePlugins;

    public WebGameController(
            GameService gameService,
            GameCatalog gameCatalog,
            UserValidationClient userValidationClient,
            List<GamePlugin> gamePlugins
    ) {
        this.gameService = gameService;
        this.gameCatalog = gameCatalog;
        this.userValidationClient = userValidationClient;
        this.gamePlugins = gamePlugins;
    }

    // -------------------------------------------------------------
    // 1. Authentification Web (Login / Logout)
    // -------------------------------------------------------------

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal UUID userId,
                            @RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (userId != null) {
            return "redirect:/web/games";
        }
        model.addAttribute("hasError", error != null);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal UUID userId,
                               @RequestParam(value = "error", required = false) String error,
                               Model model) {
        if (userId != null) {
            return "redirect:/web/games";
        }
        model.addAttribute("hasError", error != null);
        return "register";
    }

    @PostMapping("/auth/register")
    public String doRegister(@RequestParam(value = "username", required = false) String username,
                             @RequestParam(value = "email", required = false) String email,
                             @RequestParam(value = "password", required = false) String password,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Le nom d'utilisateur et le mot de passe sont obligatoires.");
            return "redirect:/web/register?error=true";
        }

        java.util.Map<String, Object> created = userValidationClient.register(username.trim(), email != null ? email.trim() : "", password);
        if (created == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de créer le compte (nom d'utilisateur déjà pris ou service 'users' indisponible).");
            return "redirect:/web/register?error=true";
        }

        // Connexion automatique après inscription réussie
        String token = userValidationClient.login(username.trim(), password);
        if (token != null) {
            Cookie cookie = new Cookie("jwt_token", token);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            cookie.setHttpOnly(true);
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
            return "redirect:/web/games";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/web/login";
    }

    @PostMapping("/auth/login")
    public String doLogin(@RequestParam(value = "username", required = false) String username,
                          @RequestParam(value = "password", required = false) String password,
                          @RequestParam(value = "directToken", required = false) String directToken,
                          HttpServletResponse response,
                          RedirectAttributes redirectAttributes) {

        String token = null;

        // Cas 1 : Token JWT collé directement
        if (directToken != null && !directToken.isBlank()) {
            token = directToken.trim();
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }
        // Cas 2 : Connexion via login/mot de passe auprès du microservice users:8081
        else if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            token = userValidationClient.login(username.trim(), password);
        }

        if (token != null && !token.isBlank()) {
            // On stocke le token JWT dans un cookie sécurisé pour le navigateur
            Cookie cookie = new Cookie("jwt_token", token);
            cookie.setPath("/");
            cookie.setMaxAge(86400); // 24h
            cookie.setHttpOnly(true);
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);

            return "redirect:/web/games";
        }

        redirectAttributes.addFlashAttribute("error", "Identifiants invalides ou service utilisateurs inaccessible");
        return "redirect:/web/login?error=true";
    }

    @GetMapping("/auth/logout")
    public String doLogout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return "redirect:/web/login";
    }

    // -------------------------------------------------------------
    // 2. Gestion et Liste des Parties
    // -------------------------------------------------------------

    @GetMapping("/games")
    public String listGames(@AuthenticationPrincipal UUID userId, Model model, Locale locale) {
        if (userId == null) {
            return "redirect:/web/login";
        }

        try {
            List<Game> userGames = gameService.getGamesForUser(userId);
            Collection<GameCatalogItem> catalogItems = gameCatalog.get(locale != null ? locale : Locale.FRENCH);

            model.addAttribute("userId", userId);
            model.addAttribute("games", userGames != null ? userGames : Collections.emptyList());
            model.addAttribute("catalog", catalogItems != null ? catalogItems : Collections.emptyList());
            return "games-list";
        } catch (Exception e) {
            model.addAttribute("userId", userId);
            model.addAttribute("games", Collections.emptyList());
            model.addAttribute("catalog", Collections.emptyList());
            model.addAttribute("error", "Erreur lors du chargement des parties : " + e.getMessage());
            return "games-list";
        }
    }

    @GetMapping("/games/new")
    public String newGameForm(@AuthenticationPrincipal UUID userId,
                              @RequestParam(value = "gameType", required = false) String gameType,
                              Model model,
                              Locale locale) {
        if (userId == null) {
            return "redirect:/web/login";
        }

        Collection<GameCatalogItem> catalogItems = gameCatalog.get(locale != null ? locale : Locale.FRENCH);
        model.addAttribute("userId", userId);
        model.addAttribute("catalog", catalogItems);
        model.addAttribute("selectedGameType", gameType != null ? gameType : "tictactoe");
        return "games-new";
    }

    @PostMapping("/games/new")
    public String createGame(@AuthenticationPrincipal UUID userId,
                             @RequestParam("gameType") String gameType,
                             @RequestParam(value = "playerCount", required = false, defaultValue = "2") Integer playerCount,
                             @RequestParam(value = "boardSize", required = false) Integer boardSize,
                             @RequestParam(value = "opponent", required = false) String opponent,
                             @RequestParam(value = "opponentId", required = false) String opponentId,
                             RedirectAttributes redirectAttributes) {
        if (userId == null) {
            return "redirect:/web/login";
        }

        try {
            GameCreationParams params = new GameCreationParams();
            params.setGameType(gameType);
            params.setPlayerCount(playerCount);
            params.setBoardSize(boardSize);

            String rawOpponent = (opponent != null && !opponent.isBlank()) ? opponent : opponentId;
            if (rawOpponent != null && !rawOpponent.isBlank()) {
                String trimmed = rawOpponent.trim();
                UserValidationClient.UserInfo found = userValidationClient.findUser(trimmed);
                if (found != null && found.id() != null) {
                    if (found.id().equals(userId)) {
                        redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas jouer contre vous-même ! Choisissez un autre joueur ou laissez vide.");
                        return "redirect:/web/games/new?gameType=" + gameType;
                    }
                    params.setOpponentIds(List.of(found.id()));
                } else {
                    redirectAttributes.addFlashAttribute("error", "Adversaire introuvable : aucun joueur ne correspond à '" + trimmed + "'. Vérifiez le nom d'utilisateur, l'email ou l'UUID.");
                    return "redirect:/web/games/new?gameType=" + gameType;
                }
            }

            Game created = gameService.createGame(userId, params);
            return "redirect:/web/games/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Impossible de créer la partie : " + e.getMessage());
            return "redirect:/web/games/new?gameType=" + gameType;
        }
    }

    // -------------------------------------------------------------
    // 3. Arène de Jeu (Plateau interactif & Coups)
    // -------------------------------------------------------------

    @GetMapping("/games/{gameId}")
    public String showGame(@AuthenticationPrincipal UUID userId,
                           @PathVariable("gameId") UUID gameId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (userId == null) {
            return "redirect:/web/login";
        }

        Game game = gameService.getGame(gameId);
        if (game == null) {
            redirectAttributes.addFlashAttribute("error", "Partie introuvable");
            return "redirect:/web/games";
        }

        if (game.getPlayerIds() == null || !game.getPlayerIds().contains(userId)) {
            redirectAttributes.addFlashAttribute("error", "Vous ne faites pas partie de cette partie");
            return "redirect:/web/games";
        }

        boolean isLocal = GameServiceImpl.isLocalGame(game);
        boolean isMyTurn = isLocal || userId.equals(game.getCurrentPlayerId());
        Collection<CellPosition> allowedPositions = isMyTurn ? gameService.getAllowedMoves(gameId) : Collections.emptyList();

        GamePlugin plugin = gamePlugins.stream()
                .filter(p -> p.supports(game.getFactoryId()) || p.getId().equalsIgnoreCase(game.getFactoryId()))
                .findFirst()
                .orElse(null);

        boolean isColumnBased = plugin != null && plugin.isColumnBased();
        List<ColumnAction> columnActions = plugin != null
                ? plugin.buildColumnActions(game, isMyTurn, allowedPositions)
                : Collections.emptyList();
        List<List<CellView>> grid = plugin != null
                ? plugin.buildGridView(game, allowedPositions)
                : Collections.emptyList();

        model.addAttribute("userId", userId);
        model.addAttribute("game", game);
        model.addAttribute("grid", grid);
        model.addAttribute("isConnectFour", isColumnBased);
        model.addAttribute("isColumnBased", isColumnBased);
        model.addAttribute("columnActions", columnActions);
        model.addAttribute("isLocal", isLocal);
        model.addAttribute("isMyTurn", isMyTurn);
        model.addAttribute("status", game.getStatus().name());

        return "game-board";
    }

    @PostMapping("/games/{gameId}/moves")
    public String playMove(@AuthenticationPrincipal UUID userId,
                           @PathVariable("gameId") UUID gameId,
                           @RequestParam("x") int x,
                           @RequestParam("y") int y,
                           RedirectAttributes redirectAttributes) {
        if (userId == null) {
            return "redirect:/web/login";
        }

        try {
            gameService.playMove(userId, gameId, new MoveParams(x, y));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Coup non autorisé : " + e.getMessage());
        }

        return "redirect:/web/games/" + gameId;
    }
}
