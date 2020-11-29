package io;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Trainer {
    private String trainerApp;
    private String trainerParameters;
    private List<HotKey> hotKeys;

    public String getTrainerApp() {
        return trainerApp;
    }

    public List<HotKey> getHotKeys() {
        return hotKeys;
    }

    public String getTrainerParameters() {
        return trainerParameters;
    }

    public String findTrainerApp(String gameDir) {
        String trainerPath = getTrainerApp();
        if (!Paths.get(trainerPath).toFile().exists()) {
            trainerPath = String.format("%s/%s/%s", gameDir, "trainers", getTrainerApp());
        }
        return trainerPath;
    }

}
