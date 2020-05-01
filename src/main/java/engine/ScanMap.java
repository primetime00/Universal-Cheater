package engine;

import cheat.AOB;
import com.sun.jna.Memory;
import io.Cheat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.Script;
import util.FormatTools;
import util.SearchTools;

import java.util.*;

public class ScanMap extends HashMap<AOB, Set<Cheat>> {
    static Logger log = LoggerFactory.getLogger(ScanMap.class);
    private static ScanMap instance;
    private int cheatListHash = -1;
    private int scriptHash = -1;
    private List<Runnable> updateHandlers = new ArrayList<>();

    static public ScanMap get() {
        if (instance == null)
            instance = new ScanMap();
        return instance;
    }

    static public void reset() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }

    public boolean update(List<Cheat> cheatList, List<Script> scriptList) {
        boolean modified = false;
        int newCLHash = cheatList == null ? cheatListHash : cheatList.hashCode();
        List<Cheat> scriptCheats = generateScriptCheats(scriptList);
        int newSHash = scriptCheats == null ? scriptHash : scriptCheats.hashCode();

        if (cheatList != null) {
            if (cheatListHash != newCLHash) {
                modified = true;
                cheatListHash = newCLHash;
                for (Cheat c : cheatList) {
                    Set<Cheat> cheatSet;
                    if (!containsKey(c.getScan())) {
                        cheatSet = new HashSet<>();
                        put(c.getScan(), cheatSet);
                    }
                    cheatSet = get(c.getScan());
                    cheatSet.add(c);
                }
            }
        }
        if (scriptCheats != null) {
            if (scriptHash != newSHash) {
                modified = true;
                scriptHash = newSHash;
                for (Cheat c : scriptCheats) {
                    Set<Cheat> cheatSet;
                    if (!containsKey(c.getScan())) {
                        cheatSet = new HashSet<>();
                        put(c.getScan(), cheatSet);
                    }
                    cheatSet = get(c.getScan());
                    cheatSet.add(c);
                }
            }
        }
        if (modified) {
            updateHandlers.forEach(Runnable::run);
        }
        return modified;
    }

    private List<Cheat> generateScriptCheats(List<Script> scriptList) {
        if (scriptList == null)
            return null;
        List<Cheat> cList = new ArrayList<>();
        for (Script s: scriptList) {
            cList.addAll(s.getAllCheats());
        }
        return cList;
    }


    public void search(long pos, Memory mem) {
        forEach((key, cheats) -> {
            if (cheats.stream().noneMatch(cheat -> cheat.getResults().getValidList(pos).size() == 0 || !cheat.operationsComplete())) { //we'll do not need to scan this set
                return;
            }
            //scan one time for this entire set;
            log.debug("Scanning for {}", cheats.iterator().next().getName());
            List<ArraySearchResult> results = SearchTools.aobSearch(key, pos, mem);
            cheats.forEach(cheat -> cheat.getResults().addAll(results, pos));
        });

        processOperations(pos, mem);

    }

    private void processOperations(long pos, Memory mem) {
        values().forEach(cheatSet ->
                cheatSet.forEach(cheat -> {
                    if (cheat.hasOperations() && !cheat.operationsComplete()) {
                        cheat.getCodes().forEach(e -> e.processOperations(cheat.getResults(), pos, mem));
                    }
                }));
    }

    public void write(List<Cheat> cheatList, List<Script> scriptList) {
        if (cheatList != null) {
            for (Cheat cheat : cheatList) {
                if (!cheat.hasWritableCode())
                    continue;
                if (!cheat.verify())
                    continue;
                if (!cheat.isTriggered())
                    continue;
                if (cheat.getResults().getAllValidList().size() > 1) {
                    log.info("---CHEAT {} has more than one result!", cheat.getName());
                    cheat.getResults().getAllValidList().forEach(r -> log.info("Address: {}", FormatTools.valueToHex(r.getAddress())));
                }
                cheat.writeCodes();
            }
        }
        if (scriptList != null) {
            for (Script script: scriptList) {
                for (Cheat cheat : script.getAllCheats()) {
                    if (!cheat.hasWritableCode())
                        continue;
                    if (!cheat.verify()) {
                        try {
                            script.handleScriptCheatFailed(cheat);
                        } catch (Exception e) {
                            log.warn("Failed to handle failed cheat {}: {}", cheat.getName(), e.getMessage());
                        }
                        continue;
                    }
                    if (!cheat.isTriggered())
                        continue;
                    int codesWritten = cheat.writeCodes();
                    if (codesWritten > 0) {
                        try {
                            script.handleScriptCheatSuccess(cheat, codesWritten, cheat.getCodes().size());
                        } catch (Exception e) {
                            log.warn("Failed to handle successful cheat {}: {}", cheat.getName(), e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public List<Cheat> getEveryCheat() {
        List<Cheat> cheats = new ArrayList<>();
        values().forEach(cheatSet -> cheatSet.forEach(cheats::add));
        return cheats;
    }

    public void addUpdateHandler(Runnable run) {
        updateHandlers.add(run);
    }
}
