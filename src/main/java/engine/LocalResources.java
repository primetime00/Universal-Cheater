package engine;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalResources {
    static Logger log = LoggerFactory.getLogger(LocalResources.class);
    Map<String, List<String>> files;

    public LocalResources() {
        files = new HashMap<>();
    }

    public void addDirectory(String dir) {
        files.put(dir, new ArrayList<>());
    }

    public void addFile(String dir, String file) {
        if (!files.containsKey(dir))
            addDirectory(dir);
        if (!files.get(dir).contains(file))
            files.get(dir).add(file);
    }

    public void process() {
        final File f = new File("cheats");
        if (!f.exists()) {
            f.mkdirs();
        }
        files.forEach((key, value) -> {
            File d = new File(f, key);
            if (!d.exists()) {
                d.mkdirs();
            }
            value.forEach(file -> {
                File c = new File(d, file);
                if (!c.exists())
                    createCheatFile(c, key, file);
            });
        });

    }

    private void createCheatFile(File c, String key, String file) {
        URL url = getClass().getResource(String.format("/cheat_codes/%s/%s", key, file));
        try (FileOutputStream os = new FileOutputStream(c)) {
            ByteStreams.copy(url.openStream(), os);
        } catch (Exception e) {
            log.error("Could not copy cheat file: {}", e.getMessage());
            e.printStackTrace();
        }


    }
}
