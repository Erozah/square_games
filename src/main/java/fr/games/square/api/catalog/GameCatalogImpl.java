package fr.games.square.api.catalog;

import fr.games.square.api.plugin.GamePlugin;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class GameCatalogImpl implements GameCatalog {

    private final List<GamePlugin> plugins;

    public GameCatalogImpl(List<GamePlugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public Collection<String> get() {
        return plugins.stream()
                .map(GamePlugin::getId)
                .toList();
    }

    @Override
    public Collection<GameCatalogItem> get(Locale locale) {
        Locale targetLocale = (locale != null) ? locale : Locale.getDefault();
        return plugins.stream()
                .map(plugin -> new GameCatalogItem(plugin.getId(), plugin.getName(targetLocale)))
                .toList();
    }
}
