package fr.games.square.api.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Locale;

@Tag(name = "Catalogue", description = "Types de jeux disponibles")
@RestController
public class GameCatalogController {

    private final GameCatalog gameCatalog;

    public GameCatalogController(GameCatalog gameCatalog) {
        this.gameCatalog = gameCatalog;
    }

    @Operation(summary = "Lister les types de jeux disponibles")
    @GetMapping("/catalog")
    public Collection<GameCatalogItem> getGameCatalog(
            @RequestHeader(value = "Accept-Language", required = false) Locale locale
    ) {
        return gameCatalog.get(locale != null ? locale : Locale.getDefault());
    }
}
