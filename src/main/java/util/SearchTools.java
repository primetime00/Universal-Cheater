package util;

import cheat.AOB;
import com.sun.jna.Memory;
import io.Cheat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchTools {
    public static Logger log = LoggerFactory.getLogger(SearchTools.class);
    static public List<ArraySearchResult> aobSearch(AOB aob, long base, Memory mem, long size) {
        return aobSearch(aob, base, 0, size, mem);
    }

    static public List<ArraySearchResult> aobSearch(AOB aob, long base, int offsetStart, long size, Memory mem) {
        long index = offsetStart;
        List<ArraySearchResult> results = new ArrayList<>();
        long end = Math.min(offsetStart+size, size);
        while (index+aob.size() < end) {
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


    static public List<ArraySearchResult> aobSearch(Cheat cheat, long base, Memory mem, long size) {
        return aobSearch(cheat.getScan(), base, mem, size);
    }

    static public List<ArraySearchResult> search(Memory memory, long size, long base, String byteString) {
        AOB aob = AOBTools.createAOB(byteString);
        return aobSearch(aob, base, memory, size);
    }

    static public List<ArraySearchResult> search(Memory memory, long base, int offsetStart, int size, String byteString) {
        AOB aob = AOBTools.createAOB(byteString);
        return aobSearch(aob, base, offsetStart, size, memory);
    }




}
