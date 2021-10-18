package io.memory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import engine.Process;
import script.Value;

public class MemoryOperation {
    protected long address;
    protected Value value;
    protected String operation;

    public boolean handle() {
        Memory mem = value.getMemory();
        Kernel32.INSTANCE.ReadProcessMemory(Process.getHandle(), new Pointer(address), mem, (int) mem.size(), null);
        if (operation.toLowerCase().equals("equal")) {
            return handleEquals(mem);
        }
        return false;

    }

    private boolean handleEquals(Memory mem) {
        return value.equals(mem.getByteArray(0, value.size()));
    }
}
