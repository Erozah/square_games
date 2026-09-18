package fr.games.square.api.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Locale;

@RestController
public class GameCatalogController {

    private final GameCatalog gameCatalog;

    public GameCatalogController(GameCatalog gameCatalog) {
        this.gameCatalog = gameCatalog;
    }

    @GetMapping("/catalog")
    public Collection<GameCatalogItem> getGameCatalog(
            @RequestHeader(value = "Accept-Language", required = false) Locale locale
    ) {
        return gameCatalog.get(locale != null ? locale : Locale.getDefault());
    }
}
