package fr.games.square.api.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Locale;

@Tag(name = "Catalogue", description = "Types de jeux disponibles")
@Controller
public class GameCatalogController {

    private final GameCatalog gameCatalog;

    public GameCatalogController(GameCatalog gameCatalog) {
        this.gameCatalog = gameCatalog;
    }

    /**
     * Endpoint API REST : renvoie les données en JSON
     */
    @Operation(summary = "Lister les types de jeux disponibles (JSON)")
    @GetMapping(value = "/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Collection<GameCatalogItem> getGameCatalog(Locale locale) {
        return gameCatalog.get(locale != null ? locale : Locale.FRENCH);
    }

    /**
     * Vue Web Thymeleaf : génère la page HTML via templates/catalog.html
     */
    @Operation(summary = "Afficher la page web du catalogue (Thymeleaf)")
    @GetMapping(value = {"/catalog/view", "/catalog.html"})
    public String getGameCatalogView(Model model, Locale locale) {
        Locale targetLocale = (locale != null) ? locale : Locale.FRENCH;
        model.addAttribute("jeux", gameCatalog.get(targetLocale));
        return "catalog";
    }

    /**
     * Route de compatibilité pour accès navigateur direct à /catalog
     */
    @GetMapping(value = "/catalog", produces = MediaType.TEXT_HTML_VALUE)
    public String getGameCatalogViewFromRoot(Model model, Locale locale) {
        return getGameCatalogView(model, locale);
    }
}
