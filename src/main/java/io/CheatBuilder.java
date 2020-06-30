package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.Script;

import java.util.ArrayList;
import java.util.List;

public class CheatBuilder {
    static Logger log = LoggerFactory.getLogger(CheatBuilder.class);
    private String name;
    private String scan;
    protected List<Code> codes;
    private boolean enabled = true;
    private int identity = 0;
    private boolean multiMatch;
    private boolean stopSearchOnResult;
    private boolean resetOnWrite;
    private Trigger trigger;
    private List<Integer> parents;
    private String scriptFile;
    private Script script;

    public CheatBuilder(String name, String scan) {
        this.name = name;
        this.scan = scan;
        this.resetOnWrite = false;
        this.multiMatch = true;
        this.stopSearchOnResult = true;
        this.parents = new ArrayList<>();
        this.codes = new ArrayList<>();
    }

    public CheatBuilder addCode(Code c) {
        codes.add(c);
        return this;
    }

    public CheatBuilder setEnabled(boolean en) {
        this.enabled = en;
        return this;
    }

    public CheatBuilder setMultiMatch(boolean m) {
        this.multiMatch = m;
        return this;
    }

    public CheatBuilder setStopSearchOnResult(boolean s) {
        this.stopSearchOnResult = s;
        return this;
    }

    public CheatBuilder setResetOnWrite(boolean r) {
        this.resetOnWrite = r;
        return this;
    }


    public CheatBuilder setIdentity(int identity) {
        this.identity = identity;
        return this;
    }

    public CheatBuilder setTrigger(Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    public CheatBuilder addParent(int identity) {
        parents.add(identity);
        return this;
    }

    public CheatBuilder setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
        return this;
    }

    public CheatBuilder setScript(Script script) {
        this.script = script;
        return this;
    }

    public Cheat build() {
        if (script != null)
            return new Cheat(name, scan, codes, enabled, identity, multiMatch, resetOnWrite, stopSearchOnResult, parents, trigger, script);
        return new Cheat(name, scan, codes, enabled, identity, multiMatch, resetOnWrite, stopSearchOnResult, parents, trigger, scriptFile);

    }
}
