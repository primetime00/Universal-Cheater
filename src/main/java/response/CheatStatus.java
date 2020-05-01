package response;


import io.Cheat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CheatStatus extends Response {
    private List<Cheat> cheatList;
    private int state;
    private String system;
    private String cht;
    private String game;
    private int hash;

    public CheatStatus() {
        super(Response.STATUS_SUCCESS);
        this.cheatList = new ArrayList<>();
        this.state = 0;
        this.system = "";
        this.cht = "";
        this.game = "";
        this.hash = hashCode();
    }

    public CheatStatus(List<Cheat> list, String system, String game, String cht) {
        super(Response.STATUS_SUCCESS);
        if (list == null) {
            this.cheatList = new ArrayList<>();
            this.state = 0;
        }
        else {
            this.cheatList = list;
            this.state = 2;
        }
        this.system = system;
        this.cht = cht;
        this.game = game;
        this.hash = hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheatStatus that = (CheatStatus) o;
        return state == that.state &&
                Objects.equals(cheatList, that.cheatList) &&
                Objects.equals(system, that.system) &&
                Objects.equals(cht, that.cht) &&
                Objects.equals(game, that.game);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + state;
        hash = 31 * hash + (system == null ? 0 : system.hashCode());
        hash = 31 * hash + (cht == null ? 0 : cht.hashCode());
        hash = 31 * hash + (game == null ? 0 : game.hashCode());
        if (cheatList != null) {
            for (Cheat c : cheatList) {
                hash = 31 * hash + c.webHashCode();
            }
        }
        return hash;
    }

}
