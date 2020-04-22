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
    static public List<ArraySearchResult> aobSearch(AOB aob, long base, Memory mem) {
        long index = 0;
        List<ArraySearchResult> results = new ArrayList<>();
        while (index+aob.size() < mem.size()) {
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

    static public List<ArraySearchResult> aobSearch(Cheat cheat, long base, Memory mem) {
        return aobSearch(cheat.getScan(), base, mem);
    }

    static public void search(Cheat cheat, Memory mem, long base) {
        if (cheat.operationsComplete() && cheat.getResults().getValidList(base).size() > 0)
            return;
        cheat.getResults().addAll(aobSearch(cheat.getScan(), base, mem), base);
        if (cheat.hasOperations()) {
            cheat.getCodes().forEach(code -> {
                if (!code.operationsComplete()) {
                    code.processOperations(cheat.getResults(), base, mem);
                }
            });
        }
    }




}
