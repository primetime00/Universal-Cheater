package script;

import java.util.*;

public class Monitor {
    private int offset;
    private int size;
    private Map<ArraySearchResult, Long> lastReads;

    public Monitor(int offset, int size) {
        this.offset = offset;
        this.size = size;
        this.lastReads = new HashMap<>();
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public boolean hasChanged(ArraySearchResult r) {
        boolean changed = false;
        long v = r.readValue(offset, size);
        changed = !lastReads.containsKey(r) || lastReads.get(r) != v;
        lastReads.put(r, v);
        return changed;
    }

    public long getValue(ArraySearchResult r) {
        return lastReads.get(r);
    }

    public void reset() {
        lastReads.clear();
    }
}
