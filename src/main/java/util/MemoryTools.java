package util;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;
import io.Cheat;
import io.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.Value;

import java.util.List;

public class MemoryTools {
    public static Logger log = LoggerFactory.getLogger(MemoryTools.class);
    static public void writeCode(Code code, List<ArraySearchResult> results) {
        results.forEach(result -> {
            code.getOffsets().forEach(ovPair -> {
                long code_address = result.getAddress() + ovPair.getOffset();
                Memory mem = ovPair.getValue().getMemory();
                Kernel32.INSTANCE.WriteProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
            });
        });
    }

    static public boolean writeCheat(Cheat cheat) {
        cheat.verify();
        if (!cheat.hasWritableCode())
            return false;
        List<ArraySearchResult> results = cheat.getResults().getAllValidList();
        if (results == null) {
            log.error("Results aren't created");
            return false;
        }
        if (results.size() == 0) {
            return false;
        }
        boolean written = false;
        for (Code code : cheat.getCodes()) {
            if (!code.operationsComplete())
                continue;
            results.forEach(result -> {
                code.getOffsets().forEach(ovPair -> {
                    long code_address = result.getAddress() + ovPair.getOffset();
                    Memory mem = ovPair.getValue().getMemory();
                    Kernel32.INSTANCE.WriteProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
                });
            });
            written = true;
        }
        return written;
    }

    static public byte[] readBytes(Code c, ArraySearchResult result, int size) {
        long code_address = result.getAddress() + c.getFirstOffset();
        Memory mem = new Memory(size);
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
        return mem.getByteArray(0, size);
    }



}
