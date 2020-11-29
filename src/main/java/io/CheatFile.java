package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CheatFile {
    static Logger log = LoggerFactory.getLogger(CheatFile.class);
    String game;
    String process;
    List<RegionSizeRange> regionSize;
    List<io.Cheat> cheats;
    List<String> scripts;
    Trainer trainer;

    public String getGame() {
        return game;
    }

    public List<io.Cheat> getCheats() {
        return cheats;
    }

    public List<RegionSizeRange> getRegionSize() {
        return regionSize;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public List<String> getScripts() {
        return scripts;
    }

    public String getProcess() {
        return process;
    }
}
