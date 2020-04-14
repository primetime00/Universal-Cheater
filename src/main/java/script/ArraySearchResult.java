package script;

import cheat.AOB;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;
import util.FormatTools;

import java.util.Objects;

public class ArraySearchResult {
    private long offset;
    private long base;
    private AOB aob;
    private boolean valid;
    private boolean scanned;
    private Object scriptData;

    public ArraySearchResult(AOB aob, long base, long offset) {
        this.base = base;
        this.offset = offset;
        this.aob = aob;
        this.valid = true;
        this.scanned  = false;
        this.scriptData = null;
    }

    public long getOffset() {
        return offset;
    }

    public long getAddress() {
        return offset+base;
    }

    public String getByteString(Memory mem, long pos, int size) {
        byte [] bytes = mem.getByteArray(getOffset() + pos, size);
        return FormatTools.bytesToString(bytes);
    }

    public long getBytesValue(Memory mem, long pos, int size) {
        return FormatTools.memoryToValue(mem, getOffset() + pos, size);
    }

    private Memory readMemory(long pos, int size) {
        Memory mem = new Memory(size);
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(getAddress()+pos), mem, size, null);
        return mem;
    }

    public String readByteString(long pos, int size) {
        Memory mem = readMemory(pos, size);
        return FormatTools.bytesToString(mem.getByteArray(0, size));
    }

    public long readValue(long pos, int size) {
        Memory mem = readMemory(pos, size);
        return FormatTools.memoryToValue(mem, 0, size);
    }

    public ScriptCheat createCheat(String name, long pos) {
        return new ScriptCheat(name, this,  getAddress() + pos);
    }

    public Verifier createVerifier(int offset, AOB aob) {
        return new Verifier(offset, aob, getAddress());
    }

    public ScriptCheat createCheat(String name) {
        return new ScriptCheat(name, this, getAddress());
    }

    public boolean verify() {
        if (!isValid())
            return false;
        Memory mem = new Memory(aob.size());
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(getAddress()), mem, aob.size(), null);
        for (int i=0; i<aob.size(); ++i) {
            if ( aob.aobAt(i) == Short.MAX_VALUE) continue;
            if ( (mem.getByte(i) & 0xFF) != aob.aobAt(i) ) {
                ScriptTools.log.warn(String.format("Could not write cheat at 0x%X.  AOB mismatch.", getAddress()));
                setValid(false);
                return false;
            }
        }
        return true;
    }


    @Override
    public String toString() {
        return String.format("Search Result [0x%X]", getAddress());
    }

    public AOB getAob() {
        return aob;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArraySearchResult that = (ArraySearchResult) o;
        return offset == that.offset &&
                base == that.base &&
                Objects.equals(aob, that.aob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, base, aob);
    }

    public boolean isScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    public Object getScriptData() {
        return scriptData;
    }

    public void setScriptData(Object scriptData) {
        this.scriptData = scriptData;
    }
}
