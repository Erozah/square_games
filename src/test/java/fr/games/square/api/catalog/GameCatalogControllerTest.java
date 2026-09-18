package fr.games.square.api.catalog;

import fr.games.square.api.plugin.ConnectFourPlugin;
import fr.games.square.api.plugin.TaquinPlugin;
import fr.games.square.api.plugin.TicTacToePlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameCatalogControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");

        var tictactoe = new TicTacToePlugin(messageSource, 2, 3);
        var taquin = new TaquinPlugin(messageSource, 1, 4);
        var connectFour = new ConnectFourPlugin(messageSource, 2, 7);

        GameCatalog gameCatalog = new GameCatalogImpl(List.of(tictactoe, taquin, connectFour));
        GameCatalogController controller = new GameCatalogController(gameCatalog);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getCatalog_FrenchLocale_ReturnsFrenchNames() throws Exception {
        mockMvc.perform(get("/catalog").header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'tictactoe')].name").value("Morpion"))
                .andExpect(jsonPath("$[?(@.id == '15 puzzle')].name").value("Taquin"))
                .andExpect(jsonPath("$[?(@.id == 'connect4')].name").value("Puissance 4"));
    }

    @Test
    void getCatalog_EnglishLocale_ReturnsEnglishNames() throws Exception {
        mockMvc.perform(get("/catalog").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'tictactoe')].name").value("Tic-Tac-Toe"))
                .andExpect(jsonPath("$[?(@.id == '15 puzzle')].name").value("15 Puzzle"))
                .andExpect(jsonPath("$[?(@.id == 'connect4')].name").value("Connect Four"));
    }
}
