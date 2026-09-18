package fr.games.square.api.catalog;

import java.util.Collection;
import java.util.Locale;

public interface GameCatalog {
    Collection<String> get();
    Collection<GameCatalogItem> get(Locale locale);
}
