package script;

import cheat.AOB;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SearchTools;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScriptTools {
    public static Logger log = LoggerFactory.getLogger(ScriptTools.class);

    static public List<ArraySearchResult> searchArray(long base, Memory mem, AOB aob) {
        return SearchTools.aobSearch(aob, base, mem);
    }

    static public void dumpMemory(String filename, long offset, Memory mem, long size) {
        File f = new File(filename);
        String abs = f.getAbsolutePath();
        StringBuffer data = new StringBuffer();
        ByteBuffer bb = mem.getByteBuffer(offset, size).order(ByteOrder.LITTLE_ENDIAN);
        while (bb.hasRemaining()) {
            byte b = bb.get();
            data.append(String.format("%02X ", b));
        }
        try {
            Files.write(Path.of(abs), data.toString().getBytes());
        } catch (IOException e) {
            log.error("Could not write dump: {}", e.getMessage());
        }
    }
/*

    static public List<Cheat> getStatus(ScriptCheat ... cheats) {
        List<Cheat> cheatList = new ArrayList<>();
        log.debug("Cheat Length = {}", cheats.length);
        for (ScriptCheat cheat : cheats) {
            if (cheat != null) {
                log.debug("Added cheat {}", cheat.getName());
                cheatList.add(cheat);
            }
        }
        return cheatList;
    }*/
}
