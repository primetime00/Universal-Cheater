package io;

import cheat.AOB;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import engine.Process;
import engine.ScanMap;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;
import script.ScriptHandler;
import util.AOBTools;
import util.MemoryTools;

import javax.script.ScriptEngine;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
    private int identity;
    private boolean hasCheats;
    private Trigger trigger;
    private String scriptFile;
    transient private ScriptHandler scriptHandler;

    private Cheat() {
        results = new ArraySearchResultList();
        enabled = true;
    }

    public Cheat(String name, String scan, List<Code> codes, boolean enable, int identity, Trigger trigger, String scriptFile) {
        this(name, scan);
        this.codes = codes;
        this.enabled = enable;
        this.identity =identity;
        this.trigger = trigger;
        this.scriptFile = scriptFile;
        if (scriptFile != null) {
            try {
                this.scriptHandler = new ScriptHandler(scriptFile);
            } catch (Exception e) {
                log.error("Could not create script handler for cheat {}: {}", name, e.getMessage());
                this.scriptHandler = null;
                this.scriptFile = null;
            }
        }

    }

    public Cheat(String name, String scan) {
        this();
        this.name = name;
        this.scan = AOBTools.createAOB(scan);
        this.codes = new ArrayList<>();
        this.trigger = null;
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
        return Objects.hash(name, scan, codes);
    }

    public int webHashCode() { return Objects.hash(name, scan, codes, hasCheats, trigger, enabled); }

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

    public Trigger getTrigger() {
        return trigger;
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

    public boolean hasOperations(Class<? extends OperationProcessor> opClass) {
        return codes.stream().anyMatch(c -> c.hasOperations(opClass));
    }

    public List<OperationProcessor> getFilterList(Class<? extends OperationProcessor> opClass) {
        List<OperationProcessor> ops = new ArrayList<>();
        codes.stream().forEach(code -> ops.addAll(code.getFilterList(opClass)));
        return ops;
    }


    public boolean isTriggered() {
        if (trigger == null)
            return true;
        return trigger.isTriggered();
    }

    public boolean hasWritableCode() {
        if (!enabled)
            return false;
        boolean writtable;
        if (!hasOperations()) {
            writtable = results.getAllValidList().size() > 0;
        }
        else {
            writtable = codes.stream().anyMatch(Code::operationsComplete);
        }
        return writtable;
    }

    public void reset() {
        if (results.size() == 0) {
            if (codes != null)
                codes.forEach(Code::reset);
        }
        else {
            if (scriptHandler != null) {
                scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_RESET);
            }
            if (results != null)
                results.clear();
            if (codes != null)
                codes.forEach(Code::reset);
            if (identity > 0) {
                for (Cheat cheat : ScanMap.get().getEveryCheat()) {
                    for (OperationProcessor op : cheat.getFilterList(Instance.class)) {
                        Instance ins = (Instance) op;
                        if (ins.getIdentity() == identity) {
                            cheat.reset();
                        }

                    }
                }
            }
        }
    }

    public void toggle() {
        this.enabled = !enabled;
        if (scriptHandler != null) {
            scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_TOGGLE, enabled);
        }
    }

    public void trigger(Trigger.TriggerInfo info) {
        if (trigger == null) {
            log.error("Cheat {} does not have a trigger", name);
            return;
        }
        log.debug("Cheat {} is triggering", name);
        trigger.handle(info);
        if (scriptHandler != null) {
            scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_TRIGGER, info);
        }
    }

    public int getIdentity() {
        return identity;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    public void addCode(Code c) {
        codes.add(c);
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public ScriptHandler getScriptHandler() {
        return scriptHandler;
    }

    public void setScriptHandler(ScriptEngine engine, String handleType, ScriptObjectMirror func) {
        if (scriptHandler == null) {
            scriptHandler = new ScriptHandler(engine);
        }
        scriptHandler.setHandle(handleType, func);
    }

    public void setScriptHandler(String handleType, ScriptObjectMirror func) {
        if (scriptHandler == null) {
            log.error("No script handler is available to handle {} for cheat {}", handleType, name);
            return;
        }
        scriptHandler.setHandle(handleType, func);
    }


    public int writeCodes() {
        int codesWritten = 0;
        for (Code code : getCodes()) {
            if (!code.operationsComplete())
                continue;
            scriptHandle(ScriptHandler.HANDLE_TYPE.BEFORE_WRITE, code);
            MemoryTools.writeCode(code, getResults().getAllValidList());
            scriptHandle(ScriptHandler.HANDLE_TYPE.AFTER_WRITE, code);
            codesWritten++;
        }
        if (getTrigger() != null && isTriggered()) getTrigger().postHandle();
        return codesWritten;
    }

    private void scriptHandle(ScriptHandler.HANDLE_TYPE type, Code code) {
        if (scriptHandler == null)
            return;
        scriptHandler.handle(type, code);
    }

    static public class CheatDeserializer implements JsonDeserializer<Cheat> {

        @Override
        public Cheat deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject val = jsonElement.getAsJsonObject();
            Cheat c = new Cheat();
            if (!val.has("name") || !val.has("scan") || !val.has("codes")) {
                Cheat.log.error("Cannot parse cheat, not enough information");
                Cheat.log.error("{}", jsonElement.toString());
                return c;
            }
            c.name = val.get("name").getAsString();
            c.scan = jsonDeserializationContext.deserialize(val.get("scan"), AOB.class);
            c.codes = jsonDeserializationContext.deserialize(val.get("codes"), new TypeToken<ArrayList<Code>>(){}.getType());
            c.identity = val.has("identity") ? val.get("identity").getAsInt() : -1;
            c.trigger = val.has("trigger") ? jsonDeserializationContext.deserialize(val.get("trigger"), Trigger.class) : null;
            if (val.has("script")) {
                c.scriptFile = val.get("script").getAsString();
            }
            if (val.has("script") && Process.getInstance() != null) {
                try {
                    c.scriptHandler = new ScriptHandler(val.get("script").getAsString());
                } catch (Exception e) {
                    Cheat.log.error("Cannot parse cheat script {}: {}", val.get("script").getAsString(), e.getMessage());
                    c.scriptHandler  = null;
                }
            }
            return c;
        }
    }
}
