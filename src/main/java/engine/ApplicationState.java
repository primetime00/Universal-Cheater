package engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationState {
    static Logger log = LoggerFactory.getLogger(ApplicationState.class);
    private int state; //0 for system, 1 for games, 2 for cheat
    private String system;

    public ApplicationState() {
        state = 0;
        system = "";
    }

    public int getState() {
        return state;
    }

    public void setState(int st) {
        if (st > 2 || st < 0) {
            state = 0;
        }
        else {
            log.error("Attempting to set an invalid state {}", st);
            state = st;
        }
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        if (CheatApplication.populateSystems(CheatApplication.cheatDir).stream().anyMatch(e -> e.equals(system))) {
            this.system = system;
        }
        else {
            log.error("Attempting to set an invalid system {}", system);
            this.system = "";
        }
    }
}
