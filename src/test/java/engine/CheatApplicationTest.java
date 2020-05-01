package engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import response.GameList;
import script.Value;

import java.util.List;

import static org.junit.Assert.*;

public class CheatApplicationTest {
    CheatApplication app;
    Gson gson;
    private final String cheatname = "cheatTest";
    @Before
    public void setUp() throws Exception {
        gson = CheatApplication.getGson();
        app = new CheatApplication();
        Util.createCheats(cheatname);
    }


    @Test
    public void testPopulateGames() {
        GameList list = CheatApplication.populateGameList(gson, "Cheat", cheatname);
        assertEquals(4, list.getGameList().size());
        assertEquals("Test Game", list.getGameList().get(0).getGame());
        assertEquals("Test Game (Script)", list.getGameList().get(1).getGame());
        assertEquals("Unit Script Test Game", list.getGameList().get(2).getGame());
        assertEquals("Unit Test Game", list.getGameList().get(3).getGame());

    }

    @Test
    public void testPopulateSystems() {
        List<String> list = CheatApplication.populateSystems(cheatname);
        assertEquals(1, list.size());
        assertEquals("Cheat", list.get(0));
    }

}