package script;

import cheat.AOB;
import cheat.Cheat;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScriptTools {
    public static Logger log = LoggerFactory.getLogger(ScriptTools.class);

    static public List<ArraySearchResult> searchArray(long base, Memory mem, long offset, AOB aob) {
        long index = 0;
        List<ArraySearchResult> results = new ArrayList<>();
        while (index+aob.size() < mem.size()) {
            if ((mem.getByte(aob.getStartIndex()+index) & 0xFF) == aob.aobAtStart() &&
                    (mem.getByte(aob.getEndIndex()+index) & 0xFF) == aob.aobAtEnd()) {

                boolean match = true;
                for (int i=0; i<aob.size(); ++i) {
                    if (aob.aobAt(i) == Short.MAX_VALUE) continue;
                    if ((mem.getByte(i+index) & 0xFF) != aob.aobAt(i)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    results.add(new ArraySearchResult(aob, base, index));
                }
            }
            index++;
        }
        if (results.size() > 0) {
            log.debug("First matches found, making new.");
        }
        return results;
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
    }
}
