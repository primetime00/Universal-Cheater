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
import script.Monitor;
import script.Script;
import script.ScriptHandler;
import util.AOBTools;
import util.MemoryTools;

import javax.script.ScriptEngine;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Cheat {
    static Logger log = LoggerFactory.getLogger(Cheat.class);
    private String name;
    private AOB scan;
    protected List<Code> codes;
    private ReentrantLock lock;
    private Set<ArraySearchResult> validResults;
    private boolean enabled;
    private boolean multiMatch;
    private boolean stopSearchOnResult;
    private boolean resetOnWrite;
    private int id;
    private int identity;
    private boolean hasCheats;
    private Trigger trigger;
    private String scriptFile;
    private List<Integer> parents;
    private boolean simulate;
    transient private ScriptHandler scriptHandler;
    transient Set<Monitor> monitors;
    private transient boolean resetQueued = false;
    private transient boolean operationResetQueued = false;

    private Cheat() {
        validResults = new HashSet<>();
        enabled = true;
        lock = new ReentrantLock();
        monitors = new HashSet<>();
    }

    public Cheat(String name, String scan, List<Code> codes, boolean enable, int identity, boolean multiMatch, boolean resetOnWrite, boolean stopSearchOnResult, List<Integer> parents, Trigger trigger, String scriptFile) {
        this(name, scan);
        this.codes = codes;
        this.enabled = enable;
        this.identity =identity;
        this.trigger = trigger;
        this.scriptFile = scriptFile;
        this.multiMatch = multiMatch;
        this.stopSearchOnResult = stopSearchOnResult;
        this.resetOnWrite = resetOnWrite;
        this.parents = parents;
        this.simulate = false;
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

    public Cheat(String name, String scan, List<Code> codes, boolean enable, int identity, boolean multiMatch, boolean resetOnWrite, boolean stopSearchOnResult, List<Integer> parents, Trigger trigger, Script script) {
        this(name, scan);
        this.codes = codes;
        this.enabled = enable;
        this.identity =identity;
        this.trigger = trigger;
        this.scriptFile = scriptFile;
        this.multiMatch = multiMatch;
        this.stopSearchOnResult = stopSearchOnResult;
        this.resetOnWrite = resetOnWrite;
        this.parents = parents;
        this.simulate = false;
        if (script != null) {
            this.scriptHandler = new ScriptHandler(script);
        }
    }


    public Cheat(String name, String scan) {
        this();
        this.name = name;
        this.scan = AOBTools.createAOB(scan);
        this.codes = new ArrayList<>();
        this.trigger = null;
        this.parents = new ArrayList<>();
        this.simulate = false;
        this.stopSearchOnResult = true;
        this.multiMatch = true;
        this.resetOnWrite = false;
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
        return Objects.hash(name, scan);
        //return Objects.hash(name, scan, codes);
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

    public boolean isMultiMatch() {
        return multiMatch;
    }

    public boolean isStopSearchOnResult() {
        return stopSearchOnResult;
    }

    public boolean isResetOnWrite() {
        return resetOnWrite;
    }

    public boolean verify() {
        int prevResultSize = getResults().size();
        getResults().removeIf(res -> !res.verify());
        //we never had any codes
        if (prevResultSize > 0 && getResults().size() == 0) { //we just lost our codes
            log.warn("Cheat {} not found", name);
            reset();
            return false;
        }
        else return true;
    }

    public boolean parentProcessingComplete() {
        if (hasParents()) {
            List<Cheat> parentCheats = getParentCheats();
            for (Cheat c : parentCheats) {
                if (!c.parentProcessingComplete())
                    return false;
                if (c.getResults().size() == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean operationsComplete() {
        if (!enabled)
            return true;
        return codes.stream().allMatch(Code::operationsComplete);
    }

    private List<Cheat> getParentCheats() {
        return ScanMap.get().getEveryCheat().stream().filter(e->getParents().contains(e.getIdentity())).collect(Collectors.toList());
    }

    public boolean hasOperations() {
        return codes.stream().anyMatch(Code::hasOperations);
    }

    public boolean hasOperations(Class<? extends OperationProcessor> opClass) {
        return codes.stream().anyMatch(c -> c.hasOperations(opClass));
    }

    public List<OperationProcessor> getFilterList(Class<? extends OperationProcessor> opClass) {
        List<OperationProcessor> ops = new ArrayList<>();
        codes.forEach(code -> ops.addAll(code.getFilterList(opClass)));
        return ops;
    }

    public List<OperationProcessor> getOperationList() {
        List<OperationProcessor> ops = new ArrayList<>();
        codes.forEach(code -> ops.addAll(code.getOperations()));
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
        if (!parentProcessingComplete())
            return false;
        if (resetOnWrite && hasParents())
            return parentProcessingComplete();
        if (scriptHandler != null && scriptHandler.has(ScriptHandler.HANDLE_TYPE.WRITE_OVERRIDE)) {
            return (boolean) scriptHandler.handleReturn(ScriptHandler.HANDLE_TYPE.WRITE_OVERRIDE);
        }
        return getResults().size() > 0;
    }

    public void queueReset() {
        resetQueued = true;
    }

    public void reset() {
        try {
            lock.lock();
            if (validResults.size() == 0) {
                resetOperations();
            } else {
                if (scriptHandler != null) {
                    scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_RESET);
                }
                if (validResults != null) {
                    validResults.clear();
                }
                resetOperations();
                if (identity > 0) {
                    for (Cheat cheat : ScanMap.get().getEveryCheat()) {
                        for (OperationProcessor op : cheat.getFilterList(Instance.class)) {
                            Instance ins = (Instance) op;
                            if (ins.getIdentity() == identity) {
                                cheat.reset();
                            }

                        }
                        for (int parent : cheat.getParents()) {
                            if (parent == identity)
                                cheat.reset();
                        }
                    }
                }
            }
            if (monitors.size() > 0) {
                for (Monitor m : monitors) {
                    m.reset();
                }
            }
            resetQueued = false;
        } finally {
            lock.unlock();
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
            scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_TRIGGER, info, new ArrayList<>(getResults()));
        }
    }

    public int getIdentity() {
        return identity;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    public List<Integer> getParents() {
        return parents;
    }

    public void setParents(List<Integer> parents) {
        this.parents = parents;
    }

    public void addCode(Code c) {
        codes.add(c);
    }

    public void removeCodes() {
        codes.clear();
    }

    public void modifyCodeValue(int index, int pair, String value) {
        if (codes == null || codes.size() <= index)
            return;
        if (codes.get(index).getOffsets() == null || codes.get(index).getOffsets().size() <= pair)
            return;
        codes.get(index).getOffsets().get(pair).setValue(value);
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

    public boolean hasParents() {
        return parents != null && parents.size() > 0;
    }

    public Set<ArraySearchResult> getResults() {
        return validResults;
    }


    public int writeCodes() {
        int codesWritten = 0;
        try {
            lock.lock();
            Collection<ArraySearchResult> validResults = getResults();
            if (scriptHandler != null) scriptHandler.handle(ScriptHandler.HANDLE_TYPE.BEFORE_WRITE, new ArrayList<>(validResults), getCodes());
            for (Code code : getCodes()) {
                if (isSimulate()) {
                    MemoryTools.simulateWriteCode(this, code, validResults);
                } else {
                    MemoryTools.writeCode(code, validResults);
                }
                codesWritten++;
            }
            if (scriptHandler != null) scriptHandler.handle(ScriptHandler.HANDLE_TYPE.AFTER_WRITE, new ArrayList<>(validResults));
            if (getTrigger() != null && isTriggered()) getTrigger().postHandle();
            return codesWritten;
        } finally {
            lock.unlock();
        }
    }

    private void scriptHandle(ScriptHandler.HANDLE_TYPE type, Code code) {
        if (scriptHandler == null)
            return;
        scriptHandler.handle(type, code);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public String toCodeString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Codes:");
        if (getResults() != null && getResults().size() > 0) {
            for (ArraySearchResult res : getResults()) {
                for (Code c : codes) {
                    buf.append(c.toAbsoluteAddress(res.getAddress()));
                }
            }
        }
        return buf.toString();
    }

    public boolean isResetQueued() {
        return resetQueued;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void addResults(Collection<ArraySearchResult> validResults) {
        getResults().addAll(validResults);
    }

    public void resetOperations() {
        if (codes != null)
            codes.forEach(Code::reset);
        operationResetQueued = false;
    }

    public Monitor addMonitor(int offset, int size) {
        Monitor m = new Monitor(offset, size);
        monitors.add(m);
        return m;
    }

    public boolean hasMonitors() {
        return monitors.size() > 0;
    }

    public void queueOperationReset() {
        operationResetQueued = true;
    }

    public boolean isOperationResetQueued() {
        return operationResetQueued;
    }

    public void processMonitors() {
        for (ArraySearchResult r : getResults()) {
            for (Monitor m : monitors) {
                boolean change = m.hasChanged(r);
                if (scriptHandler.has(ScriptHandler.HANDLE_TYPE.ON_MONITOR_CHANGE) && change) {
                    scriptHandler.handle(ScriptHandler.HANDLE_TYPE.ON_MONITOR_CHANGE, m, r, m.getValue(r));
                }
            }
        }
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
            c.codes = val.has("codes") ? jsonDeserializationContext.deserialize(val.get("codes"), new TypeToken<ArrayList<Code>>(){}.getType()) : new ArrayList<>();
            c.identity = val.has("identity") ? val.get("identity").getAsInt() : -1;
            c.simulate = val.has("simulate") && val.get("simulate").getAsBoolean();
            c.multiMatch = !val.has("multiMatch") || val.get("multiMatch").getAsBoolean();
            c.stopSearchOnResult = !val.has("stopSearchOnResult") || val.get("stopSearchOnResult").getAsBoolean();
            c.resetOnWrite = val.has("resetOnWrite") && val.get("resetOnWrite").getAsBoolean();
            c.parents = gatherParents(val, jsonDeserializationContext);
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

        private List<Integer> gatherParents(JsonObject val, JsonDeserializationContext jsonDeserializationContext) {
            List<Integer> res = new ArrayList<>();
            if (val.has("parents") && val.get("parents").isJsonArray()) {
                res = jsonDeserializationContext.deserialize(val.get("parents"), new TypeToken<List<Integer>>() {}.getType());
            }
            return res;
        }
    }
}
