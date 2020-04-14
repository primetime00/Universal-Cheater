package cheat;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import io.CheatFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import static util.AOBTools.parseAOB;

public class MasterCode {
    static Logger log = LoggerFactory.getLogger(MasterCode.class);
    static private int maxWrites = 10;
    private short[] aob;
    private boolean researchAfterFound = true;
    private CheatFile.Search searcher;
    private CheatFile.Change changer;
    private int startIndex;
    private int endIndex;
    private boolean old;
    private int writeCounter;
    private List<StaticCheat> cheats;
    private List<Long> matches;

    public MasterCode(short[] aob) {
        this.aob = aob;
        this.cheats = new ArrayList<>();
        this.matches = new CopyOnWriteArrayList<>(); //ArrayList<>();
        this.old = true;
        this.writeCounter = 0;
        findIndex();
    }

    public List<StaticCheat> getCodes() {
        return cheats;
    }

    public List<Long> getMatches() {
        return matches;
    }

    public MasterCode(String aob, boolean researchAfterFound, CheatFile.Search search, CheatFile.Change change) {
        this(parseAOB(aob));
        this.researchAfterFound = researchAfterFound;
        this.searcher = search;
        this.changer = change;
    }

    public CheatFile.Search getSearcher() {
        return searcher;
    }

    public CheatFile.Change getChanger() {
        return changer;
    }

    private void findIndex() {
        for (int i=0; i<aob.length; ++i) {
            if (aob[i] != Short.MAX_VALUE && aob[i] > 0) {
                startIndex = i;
                break;
            }
        }
        for (int i = aob.length-1; i>= 0; --i) {
            if (aob[i] != Short.MAX_VALUE && aob[i] > 0) {
                endIndex = i;
                break;
            }
        }
    }

    public void addCheat(StaticCheat item) {
        cheats.add(item);
    }

    public void search(long pos, Memory mem) {
        long index = 0;
        while (index+aob.length < mem.size()) {

            if ((mem.getByte(startIndex+index) & 0xFF) == aob[startIndex] &&
                (mem.getByte(endIndex+index) & 0xFF) == aob[endIndex]) {
                if (foundMatch(index, mem)) {
                    log.info("Found a match! {}", String.format("%X", pos+index));
                    matches.add(pos + index);
                }
            }
            index++;
        }
        if (matches.size() > 0) {
            log.debug("First matches found, making new.");
            old = false;
            writeCounter = 0;
        }
    }

    private boolean foundMatch(long index, Memory mem) {
        for (int i=0; i<aob.length; ++i) {
            if (aob[i] == Short.MAX_VALUE) continue;
            if ((mem.getByte(i+index) & 0xFF) != aob[i])
                return false;
        }
        if (searcher != null && !searcher.isFound()) {
            return processValueSearch(index, mem);
        }
        else if (changer != null) {
            return processValueChange(index, mem);
        }
        return true;
    }

    private boolean processValueChange(long index, Memory mem) {
        if (!changer.has(index)) {
            changer.add(index);
        }
        CheatFile.Change.ChangeRecord rec = changer.get(index);
        if (!rec.started()) {
            rec.mark();
            return false;
        }
        if (!rec.expired()) {
            return false;
        }
        if (!rec.isHasRecord()) {
            rec.recordValue(mem.getByte(changer.getOffset() + index));
            return false;
        }
        if (rec.valueChanged(mem.getByte(changer.getOffset() + index))) {
            return true;
        }
        return false;
    }

    private boolean processValueSearch(long index, Memory mem) {
        int offset = searcher.getOffset();
        if (aob[offset] != Short.MAX_VALUE) {
            log.error("Searcher needs to have a wildcard value at the offset it is looking for.");
            return false;
        }
        Code low = new Code(searcher.getOffset(), searcher.getLow());
        Code high = new Code(searcher.getOffset(), searcher.getHigh());
        boolean res = false;
        switch (low.type) {
            case INT:
                res = mem.getInt(index+offset) >= low.iValue && mem.getInt(index+offset) <= high.iValue;
                break;
            case BYTE:
                res = mem.getByte(index+offset) >= low.bValue && mem.getByte(index+offset) <= high.bValue;
                break;
            case LONG:
                res = mem.getLong(index+offset) >= low.lValue && mem.getLong(index+offset) <= high.lValue;
                break;
            case FLOAT:
                res = Float.compare(mem.getFloat(index+offset), low.fValue) >= 0 && Float.compare(mem.getFloat(index+offset), high.fValue) <= 0;
                break;
            case SHORT:
                res = mem.getShort(index+offset) >= low.sValue && mem.getShort(index+offset) <= high.sValue;
                break;
            case DOUBLE:
                res = Double.compare(mem.getDouble(index+offset), low.dValue) >= 0 && Double.compare(mem.getDouble(index+offset), high.dValue) <= 0;
        }
        searcher.setFound(res);
        log.info("engine.Process search found res {}", res);
        return res;
    }

    public void write(WinNT.HANDLE handle) {
        List<Long> removes = new ArrayList<>();
        for (long addr: matches) {
            if (!verifyAOB(addr, handle)) {
                log.debug(String.format("Could not verify AOB at %X", addr));
                if (searcher != null)
                    searcher.setFound(false);
                if (changer != null)
                    changer.reset();
                removes.add(addr);
                old = true;
                continue;
            }
            cheats.forEach(cheat -> {
                if (!cheat.isEnabled())
                    return;
                cheat.getCodes().forEach(code -> {
                    Memory mem = null;
                    switch (code.type) {
                        default:
                        case INT:
                            mem = new Memory(4);
                            int[] iValue = {code.iValue};
                            mem.write(0, iValue, 0, 1);
                            break;
                        case SHORT:
                            mem = new Memory(2);
                            short[] sValue = {code.sValue};
                            mem.write(0, sValue, 0, 1);
                            break;
                        case BYTE:
                            mem = new Memory(1);
                            byte[] bValue = {code.bValue};
                            mem.write(0, bValue, 0, 1);
                            break;
                        case LONG:
                            mem = new Memory(8);
                            long[] lValue = {code.lValue};
                            mem.write(0, lValue, 0, 1);
                            break;
                        case FLOAT:
                            mem = new Memory(4);
                            float[] fValue = {code.fValue};
                            mem.write(0, fValue, 0, 1);
                            break;
                        case DOUBLE:
                            mem = new Memory(8);
                            double[] dValue = {code.dValue};
                            mem.write(0, dValue, 0, 1);
                            break;
                    }
                    Kernel32.INSTANCE.WriteProcessMemory(handle, new Pointer(addr + code.offset), mem, (int) mem.size(), null);
                });
                log.trace("Writing cheat {}", cheat.getName());
            });
        }
        matches.removeAll(removes);
        if (researchAfterFound) {
            writeCounter++;
            if (writeCounter > maxWrites) {
                log.trace("Write has become old. making old.");
                old = true;
                matches.clear();
                if (searcher != null)
                    searcher.setFound(false);
                if (changer != null)
                    changer.reset();
            }
        }
    }

    private boolean verifyAOB(long addr, WinNT.HANDLE handle) {
        Memory mem = new Memory(aob.length);
        Kernel32.INSTANCE.ReadProcessMemory(handle, new Pointer(addr), mem, aob.length, null);
        for (int i=0; i<aob.length; ++i) {
            if ( aob[i] == Short.MAX_VALUE) continue;
            if ( (mem.getByte(i) & 0xFF) != aob[i] ) {
                return false;
            }
        }
        return true;
    }

    public void addCheat(int index, String name, int offset, String value) {
        addCheat(createCheat(index, name));
    }

    private StaticCheat createCheat(int index, String name) {
        return new StaticCheat(this, index, name);
    }

    public boolean isOld() {
        return old;
    }

    public void reset() {
        log.debug("Resetting AOB");
        if (searcher != null)
            searcher.setFound(false);
        if (changer != null)
            changer.reset();
        old = true;
        matches.clear();
    }
}
