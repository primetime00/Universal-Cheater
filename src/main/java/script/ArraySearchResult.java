package script;

import cheat.AOB;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;
import io.Cheat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.AOBTools;
import util.FormatTools;

import java.nio.ByteBuffer;
import java.util.Objects;

public class ArraySearchResult {
    static Logger log = LoggerFactory.getLogger(ArraySearchResult.class);
    private long offset;
    private long base;
    private AOB aob;
    private boolean valid;
    private boolean scanned;
    private Object scriptData;
    private Object miscData;

    public ArraySearchResult(AOB aob, long base, long offset) {
        this.base = base;
        this.offset = offset;
        this.aob = aob;
        this.valid = true;
        this.scanned  = false;
        this.scriptData = null;
    }

    public ArraySearchResult(ArraySearchResult other) {
        this(other.aob, other.base, other.offset);
        this.valid = other.valid;
        this.scanned = other.scanned;
        this.scriptData = other.scriptData;
    }

    public long getOffset() {
        return offset;
    }

    public long getAddress() {
        return offset+base;
    }

    public byte[] getBytes(Memory mem, long pos, int size) {
        byte [] bytes = mem.getByteArray(getOffset() + pos, size);
        return bytes;
    }

    public String getByteString(Memory mem, long pos, int size) {
        return FormatTools.bytesToString(getBytes(mem, pos, size));
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

    public double readFloat(long pos, int size) {
        Memory mem = readMemory(pos, size);
        return FormatTools.memoryToDecimal(mem, 0, size);
    }

    public boolean verify() {
        if (!isValid())
            return false;
        if (aob.isEmpty()) {
            return true;
        }
        ByteBuffer bytes = ByteBuffer.allocate(256);
        Memory mem = new Memory(aob.size());
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(getAddress()), mem, aob.size(), null);
        for (int i=0; i<aob.size(); ++i) {
            if ( aob.aobAt(i) == Short.MAX_VALUE) continue;
            int memByte = mem.getByte(i) & 0xFF;
            if ( memByte != aob.aobAt(i) ) {
                bytes.rewind();
                log.warn(String.format("Could not write cheat at 0x%X.  AOB mismatch[%d]: %s", getAddress(), i, AOBTools.displayAOBCompare(aob, bytes.array())));
                setValid(false);
                return false;
            }
            else {
                bytes.put((byte)memByte);
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

    public Object getMiscData() {
        return miscData;
    }

    public void setMiscData(Object miscData) {
        this.miscData = miscData;
    }

    public long getBase() {
        return base;
    }
}
