package script;

import cheat.AOB;
import cheat.Cheat;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;
import util.FormatTools;

import java.util.Objects;

public class ScriptCheat extends Cheat {
    private long code_address;
    private long aob_address;
    transient private ArraySearchResult parentSearch;
    private AOB aob;

    public ScriptCheat(String name, ArraySearchResult parent, long code_address) {
        super(name);
        this.code_address = code_address;
        this.aob_address = parent.getAddress();
        this.aob = parent.getAob();
        parentSearch = parent;
        id = hashCode();
    }

    public boolean write(Value value) {
        if (!enabled) {
            ScriptTools.log.trace("Skipping cheat {} since it is not enabled.", name);
            return false;
        }
        if (!parentSearch.isValid())
            return false;
        ScriptTools.log.debug(String.format("Writing %s at 0x%X", value, code_address));
        Memory mem = value.getMemory();
        Kernel32.INSTANCE.WriteProcessMemory(Process.getHandle(), new Pointer(code_address), mem, (int) mem.size(), null);
        return true;
    }

    public Memory readMemory(long offset, int size) {
        Memory mem = new Memory(size);
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(code_address+offset), mem, size, null);
        return mem;
    }

    public long readValue(long offset, int size) {
        Memory mem = readMemory(offset, size);
        return FormatTools.memoryToValue(mem, 0, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptCheat cheat = (ScriptCheat) o;
        return code_address == cheat.code_address &&
                Objects.equals(name, cheat.name);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return Objects.hash(hash, code_address);
    }

    @Override
    public void reset() {
        parentSearch.setValid(false);
    }
}
