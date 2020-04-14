package script;

import cheat.AOB;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;

public class Verifier {
    private  int offset;
    private long address;
    private AOB aob;
    public Verifier(int offset, AOB aob, long address) {
        this.offset = offset;
        this.aob = aob;
        this.address = address;
    }

    public boolean verify() {
        Memory mem = new Memory(aob.size());
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(address), mem, aob.size(), null);
        for (int i=0; i<aob.size(); ++i) {
            if ( aob.aobAt(i) == Short.MAX_VALUE) continue;
            if ( (mem.getByte(i) & 0xFF) != aob.aobAt(i) ) {
                ScriptTools.log.warn(String.format("Could not write cheat at 0x%X.  AOB mismatch.", getAddress()));
                return false;
            }
        }
        return true;
    }

    public long getAddress() {
        return address;
    }
}
