package games;

public class Game {
    private String system;
    private String game;
    private String cht;

    public Game(String system, String game, String cht) {
        this.system = system;
        this.game = game;
        this.cht = cht;
    }

    public String getSystem() {
        return system;
    }

    public String getGame() {
        return game;
    }

    public String getCht() {
        return cht;
    }
}
