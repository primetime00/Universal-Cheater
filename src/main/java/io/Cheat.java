package io;

import cheat.AOB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;
import util.AOBTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cheat {
    static Logger log = LoggerFactory.getLogger(Cheat.class);
    private String name;
    private AOB scan;
    protected List<Code> codes;
    private ArraySearchResultList results;
    private boolean enabled;
    private int id;
    private boolean hasCheats;

    private Cheat() {
        results = new ArraySearchResultList();
        enabled = true;
    }
    public Cheat(String name, String scan) {
        this();
        this.name = name;
        this.scan = AOBTools.createAOB(scan);
        this.codes = new ArrayList<>();
    }

    public Cheat(String name, String scan, Code ... codes) {
        this(name, scan);
        if (codes != null && codes.length > 0) {
            Collections.addAll(this.codes, codes);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cheat cheat = (Cheat) o;
        return Objects.equals(name, cheat.name) &&
                Objects.equals(scan, cheat.scan) &&
                Objects.equals(codes, cheat.codes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, scan, codes, hasCheats);
    }

    public void updateData() {
        id = Objects.hash(name, scan);
        hasCheats = hasWritableCode();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AOB getScan() {
        return scan;
    }

    public List<Code> getCodes() {
        return codes;
    }

    public ArraySearchResultList getResults() {
        return results;
    }

    public List<ArraySearchResult> getAllValidResults() {
        if (results != null) {
            return results.getAllValidList();
        }
        return new ArrayList<>();
    }

    public int getNumberOfValidResults() {
        if (results != null) {
            return results.getAllValidList().size();
        }
        return 0;
    }




    public boolean verify() {
        int prevResultSize = results.getAllValidList().size();
        for (ArraySearchResult res : results.getAllValidList()) {
            res.verify();
        }
        //we never had any codes
        if (prevResultSize > 0 && results.getAllValidList().size() == 0) { //we just lost our codes
            log.warn("Cheat {} not found", name);
            reset();
            return false;
        }
        else return results.getAllValidList().size() != 0;
    }

    public boolean operationsComplete() {
        if (!enabled)
            return true;
        return codes.stream().allMatch(Code::operationsComplete);
    }

    public boolean hasOperations() {
        return codes.stream().anyMatch(Code::hasOperations);
    }

    public boolean hasWritableCode() {
        if (!enabled)
            return false;
        if (!hasOperations()) {
            return results.getAllValidList().size() > 0;
        }
        return codes.stream().anyMatch(Code::operationsComplete);
    }

    public void reset() {
        if (results != null)
            results.clear();
        if (codes != null)
            codes.forEach(Code::reset);

    }

    public void toggle() {
        this.enabled = !enabled;
    }

    public void addCode(Code c) {
        codes.add(c);
    }

}
