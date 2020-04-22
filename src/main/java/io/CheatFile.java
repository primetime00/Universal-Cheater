package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CheatFile {
    static Logger log = LoggerFactory.getLogger(CheatFile.class);
    String game;
    Window window;
    List<Long> regionSize;
    List<io.Cheat> cheats;
    List<String> scripts;

    public String getGame() {
        return game;
    }

    public Window getWindow() {
        return window;
    }

    public List<io.Cheat> getCheats() {
        return cheats;
    }

    public List<Long> getRegionSize() {
        return regionSize;
    }

    public List<String> getScripts() {
        return scripts;
    }

    static public class Window {
        private String windowClass;
        private String windowTitle;
        private boolean partialMatch;

        public String getWindowClass() {
            return windowClass;
        }

        public String getWindowTitle() {
            return windowTitle;
        }

        public boolean isPartialMatch() {
            return partialMatch;
        }

    }

}
