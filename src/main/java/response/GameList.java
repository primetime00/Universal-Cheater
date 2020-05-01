package response;

import games.Game;

import java.util.List;

public class GameList extends Response {
    List<Game> gameList;

    public GameList(List<Game> list) {
        super(list == null || list.size() == 0 ?  STATUS_FAIL : STATUS_SUCCESS);
        if (list == null || list.size() == 0) {
            this.message = "No games available.";
        }
        this.gameList = list;
    }

    public List<Game> getGameList() {
        return gameList;
    }
}
