package engine;

import cheat.AOB;
import com.sun.jna.Memory;
import io.Cheat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.Script;
import script.ScriptHandler;
import util.FormatTools;
import util.MemoryTools;
import util.SearchTools;

import java.util.*;
import java.util.stream.Collectors;

public class ScanMap {
    static Logger log = LoggerFactory.getLogger(ScanMap.class);
    private static ScanMap instance;
    private int cheatListHash = -1;
    private int scriptHash = -1;
    private List<Runnable> updateHandlers = new ArrayList<>();
    private SearchResults searchResults = new SearchResults();
    private Map<AOB, Set<Cheat>> scanMapData;

    public ScanMap() {
        scanMapData = new HashMap<>();
    }

    static public ScanMap get() {
        if (instance == null)
            instance = new ScanMap();
        return instance;
    }

    static public void reset() {
        if (instance != null) {
            instance = null;
            get();
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
                    if (!scanMapData.containsKey(c.getScan())) {
                        cheatSet = new HashSet<>();
                        scanMapData.put(c.getScan(), cheatSet);
                    }
                    cheatSet = scanMapData.get(c.getScan());
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
                    if (!scanMapData.containsKey(c.getScan())) {
                        cheatSet = new HashSet<>();
                        scanMapData.put(c.getScan(), cheatSet);
                    }
                    cheatSet = scanMapData.get(c.getScan());
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


    public void search(long pos, Memory mem, long size) {
        //searchResults.clear(pos);
        for (Map.Entry<AOB, Set<Cheat>> entry : scanMapData.entrySet()) {
            Set<Cheat> cheats = entry.getValue();
            AOB key = entry.getKey();
            //do we need to search for this AOB?
            if (cheats.stream().allMatch(e -> !e.isMultiMatch() && e.getResults().size() > 0))
                continue;
            if (cheats.stream().allMatch(e -> e.isStopSearchOnResult() && e.getResults().size() > 0))
                continue;
            if (cheats.stream().noneMatch(Cheat::parentProcessingComplete))
                continue;

            //scan one time for this entire set;
            List<ArraySearchResult> results = SearchTools.aobSearch(key, pos, mem, size);
            if (log.isTraceEnabled()) {
                log.trace("Searched {}-{} AOB {} has {} results", FormatTools.valueToHex(pos), FormatTools.valueToHex(pos + size), key.toString(), results.size());
            }
            if (results.size() > 0) {
                for (Cheat cheat : cheats) {
                    if (!cheat.parentProcessingComplete())
                        continue;
                    if (cheat.isStopSearchOnResult() && cheat.getResults().size() > 0)
                        continue;
                    searchResults.addResults(pos, cheat, results);
                    if (cheat.getScriptHandler() != null) {
                        List<ArraySearchResult> res = searchResults.getResults(cheat);
                        cheat.getScriptHandler().handle(ScriptHandler.HANDLE_TYPE.ON_RESULTS, res, pos, mem);
                    }
                }
            }
        }
        searchResults.processOperations(pos, mem);


    }


    public void write(List<Cheat> cheatList, List<Script> scriptList) {
        List<Cheat> staticCheats = new ArrayList<>();
        Map<Script, List<Cheat>> scriptCheats = new HashMap<>();
        if (cheatList != null) {
            for (Cheat cheat : cheatList) {
                if (!cheat.isEnabled())
                    continue;
                if (!cheat.verify())
                    continue;
                if (cheat.getResults().size() == 0)
                    continue;
                if (cheat.hasMonitors())
                    cheat.processMonitors();
                if (!cheat.isTriggered())
                    continue;
                if (cheat.isResetQueued()) {
                    cheat.reset();
                    continue;
                }
                staticCheats.add(cheat);
            }
        }
        if (scriptList != null) {
            for (Script script: scriptList) {
                for (Cheat cheat : script.getAllCheats()) {
                    if (!cheat.isEnabled())
                        continue;
                    if (cheat.getCodes().size() == 0) {
                        if (cheat.isResetQueued())
                            cheat.reset();
                        continue;
                    }
                    if (!cheat.verify()) {
                        try {
                            script.handleScriptCheatFailed(cheat);
                        } catch (Exception e) {
                            log.warn("Failed to handle failed cheat {}: {}", cheat.getName(), e.getMessage());
                        }
                        continue;
                    }
                    if (cheat.getResults().size() == 0)
                        continue;
                    if (cheat.hasMonitors())
                        cheat.processMonitors();
                    if (!cheat.isTriggered())
                        continue;
                    if (cheat.isResetQueued()) {
                        cheat.reset();
                        continue;
                    }
                    if (!scriptCheats.containsKey(script))
                        scriptCheats.put(script, new ArrayList<>());
                    scriptCheats.get(script).add(cheat);
                }
            }
        }
        for (Cheat c : staticCheats) {
            c.writeCodes();
            if (c.isResetOnWrite())
                c.reset();
        }
        for (Map.Entry<Script, List<Cheat>> s: scriptCheats.entrySet()) {
            for (Cheat c : s.getValue()) {
                int codesWritten = c.writeCodes();
                if (c.isResetOnWrite())
                    c.reset();
                if (codesWritten > 0) {
                    try {
                        s.getKey().handleScriptCheatSuccess(c, codesWritten, c.getCodes().size());
                    } catch (Exception e) {
                        log.warn("Failed to handle successful cheat {}: {}", c.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    public List<Cheat> getEveryCheat() {
        List<Cheat> cheats = new ArrayList<>();
        scanMapData.values().forEach(cheatSet -> cheatSet.forEach(cheats::add));
        return cheats;
    }

    public void addUpdateHandler(Runnable run) {
        updateHandlers.add(run);
    }

    public List<ArraySearchResult> getAllSearchResults() {
        return searchResults.getAllResults();
    }

    public List<ArraySearchResult> getAllSearchResults(Cheat cht) {
        return searchResults.getResults(cht);
    }

    public void writeResults() {
        for (Cheat c : getEveryCheat()) {
            if (!c.parentProcessingComplete())
                continue;
            if (!c.isMultiMatch() && c.getResults().size() > 0)
                continue;
            if (c.operationsComplete()) {
                List<ArraySearchResult> validResults = getAllSearchResults(c).stream().filter(ArraySearchResult::isValid).collect(Collectors.toList());
                //do we add results, overwrite, etc?
                try {
                    c.lock();
                    c.addResults(validResults);
                    searchResults.clearResults(c);
                    c.queueOperationReset();
                } finally {
                    c.unlock();
                }
            }
        }
    }

    public void initializeAbsolutes(List<Cheat> cheatList, List<Script> scriptList) {
        if (cheatList != null) {
            for (Cheat c : cheatList) {
                if (!c.getScan().isAbsolute())
                    continue;
                List<ArraySearchResult> res = new ArrayList<ArraySearchResult>();
                res.add(new ArraySearchResult(c.getScan(), 0, 0));
                c.addResults(res);
            }
        }
        if (scriptList != null) {
            for (Script s : scriptList) {
                if (s.getAllCheats() != null) {
                    for (Cheat c : s.getAllCheats()) {
                        if (!c.getScan().isAbsolute())
                            continue;
                        List<ArraySearchResult> res = new ArrayList<ArraySearchResult>();
                        res.add(new ArraySearchResult(c.getScan(), 0, 0));
                        c.addResults(res);
                    }
                }
            }
        }
    }
}
