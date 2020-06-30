package engine;

import com.sun.jna.Memory;
import io.Cheat;
import script.ArraySearchResult;

import java.util.*;

public class SearchResults {
    Map<Long, Map<Cheat, Set<ArraySearchResult>>> searchResults;

    public SearchResults() {
        this.searchResults = new HashMap<>();
    }

    public void clear(long base) {
        if (searchResults.containsKey(base))
            searchResults.get(base).clear();
    }

    public void processOperations(long pos, Memory mem) {
        Map<Cheat, Set<ArraySearchResult>> searchElements = searchResults.get(pos);
        if (searchElements != null) {
            for (Map.Entry<Cheat, Set<ArraySearchResult>> entry : searchElements.entrySet()) {
                Cheat cheat = entry.getKey();
                if (cheat.isOperationResetQueued()) {
                    try {
                        cheat.lock();
                        cheat.resetOperations();
                    } finally {
                        cheat.unlock();
                    }
                }
                if (!cheat.parentProcessingComplete())
                    continue;
                if (cheat.isStopSearchOnResult() && cheat.getResults().size() > 0)
                    continue;
                if (cheat.hasOperations() && !cheat.operationsComplete()) {
                    cheat.getCodes().forEach(e -> e.processOperations(entry.getValue(), pos, mem));
                }
            }
        }
    }

    public List<ArraySearchResult> getAllResults() {
        List<ArraySearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, Map<Cheat, Set<ArraySearchResult>>> item:  searchResults.entrySet()) {
            for (Set<ArraySearchResult> list : item.getValue().values()) {
                results.addAll(list);
            }
        }
        return results;
    }

    public void addResults(long base, Cheat cheat, List<ArraySearchResult> results) {
        if (!searchResults.containsKey(base))
            searchResults.put(base, new HashMap<>());
        Map<Cheat, Set<ArraySearchResult>> searchElements = searchResults.get(base);
        if (!searchElements.containsKey(cheat))
            searchElements.put(cheat, new HashSet<>());
        results.forEach(res -> searchElements.get(cheat).add(new ArraySearchResult(res)));
    }

    public List<ArraySearchResult> getResults(Cheat cht) {
        List<ArraySearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, Map<Cheat, Set<ArraySearchResult>>> item:  searchResults.entrySet()) {
            if (item.getValue().get(cht) == null)
                continue;
            results.addAll(item.getValue().get(cht));
        }
        return results;
    }

    public void clearResults(Cheat c) {
        for (Map.Entry<Long, Map<Cheat, Set<ArraySearchResult>>> item:  searchResults.entrySet()) {
            if (item.getValue().get(c) == null)
                continue;
            item.getValue().get(c).clear();
        }
    }
}
