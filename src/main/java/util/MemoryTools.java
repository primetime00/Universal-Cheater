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

import java.util.Collection;

public class MemoryTools {
    public static Logger log = LoggerFactory.getLogger(MemoryTools.class);
    static public void writeCode(Code code, Collection<ArraySearchResult> results) {
        results.forEach(result -> {
            code.getOffsets().forEach(ovPair -> {
                long code_address = result.getAddress() + ovPair.getOffset();
                Memory mem = ovPair.getValue().getMemory();
                Kernel32.INSTANCE.WriteProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
            });
        });
    }

    static public boolean writeResult(ArraySearchResult result, int offset, String value) {
        if (result == null) {
            log.error("Result isn't created");
            return false;
        }
        Value v = Value.createValue(value);
        Memory mem = v.getMemory();
        Kernel32.INSTANCE.WriteProcessMemory(Process.getHandle(), new Pointer(result.getAddress()+offset), mem, (int) mem.size(), null);
        return true;
    }

    static public boolean writeCheat(Cheat cheat) {
        cheat.verify();
        if (cheat.getResults().size() == 0)
            return false;
        if (!cheat.isEnabled())
            return false;
        if (!cheat.isTriggered())
            return false;
        Collection<ArraySearchResult> results = cheat.getResults();
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


    public static void simulateWriteCode(Cheat cheat, Code code, Collection<ArraySearchResult> results) {
        results.forEach(result -> {
            code.getOffsets().forEach(ovPair -> {
                long code_address = result.getAddress() + ovPair.getOffset();
                Value v = ovPair.getValue();
                int size = v.size();
                Memory mem = new Memory(size);
                Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
                log.info("Simualate write at {} with value {}.  Currently = {}", FormatTools.valueToHex(code_address), v, FormatTools.bytesToString(mem.getByteArray(0, size)));
            });
        });
    }
}
