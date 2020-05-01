package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CheatBuilder {
    static Logger log = LoggerFactory.getLogger(CheatBuilder.class);
    private String name;
    private String scan;
    protected List<Code> codes;
    private boolean enabled = true;
    private int identity = 0;
    private Trigger trigger;
    private String scriptFile;

    public CheatBuilder(String name, String scan) {
        this.name = name;
        this.scan = scan;
    }

    public CheatBuilder addCode(Code c) {
        if (codes == null)
            codes = new ArrayList<>();
        codes.add(c);
        return this;
    }

    public CheatBuilder setEnabled(boolean en) {
        this.enabled = en;
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

    public CheatBuilder setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
        return this;
    }

    public Cheat build() {
        return new Cheat(name, scan, codes, enabled, identity, trigger, scriptFile);

    }
}
