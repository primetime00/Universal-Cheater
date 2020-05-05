package games;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game1 = (Game) o;
        return Objects.equals(system, game1.system) &&
                Objects.equals(game, game1.game) &&
                Objects.equals(cht, game1.cht);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, game, cht);
    }
}
