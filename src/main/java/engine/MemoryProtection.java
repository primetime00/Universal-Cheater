package engine;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.win32.W32APIOptions;

public interface MemoryProtection  extends Kernel32 {
        MemoryProtection INSTANCE = (MemoryProtection) Native.load("kernel32", MemoryProtection.class, W32APIOptions.DEFAULT_OPTIONS);
        int VirtualProtectEx(HANDLE handle, Pointer lpAddress, int dwSize, int flNewProtect, DWORDByReference lpflOldProtect);
}
