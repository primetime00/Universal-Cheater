package script;

import engine.Process;
import games.RunnableCheat;
import io.Cheat;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import java.util.*;

public class ScriptHandler {
    static Logger log = LoggerFactory.getLogger(ScriptHandler.class);
    private Script script;
    private ScriptEngine engine;

    public ScriptHandler(String scriptFile) throws Exception {
        this();
        this.script = createScript(scriptFile);
        this.engine = script.getEngine();
    }

    public ScriptHandler(Script script) {
        this();
        this.script = script;
        this.engine = script.getEngine();
    }

    private Script createScript(String scriptFile) throws Exception {
        if (Process.getInstance() == null) {
            throw new Exception("Cannot create script handler since no process is open");
        }
        if (Process.getInstance().getData() == null) {
            throw new Exception("Cannot create script handler since no process is open correctly");
        }
        RunnableCheat data = Process.getInstance().getData();
        return new Script(String.format("%s/%s/scripts/%s", data.getDirectory(), data.getSystem(), scriptFile));
    }


    public void initialize(Cheat c) {
        try {
            this.script.initCheat(c);
        } catch (Exception e) {
            log.error("Cannot initialize cheat {}", e.getMessage());
        }
    }

    public Script getScript() {
        return script;
    }

    public enum HANDLE_TYPE{
        BEFORE_WRITE,
        AFTER_WRITE,
        ON_RESET,
        ON_TRIGGER,
        ON_TOGGLE,
        ON_RESULTS,
        ON_MONITOR_CHANGE,
        WRITE_OVERRIDE,
        PREREAD,
        VERIFY
    };
    List<String> handleTypes;
    Map<HANDLE_TYPE, ScriptObjectMirror> handleMap;

    public ScriptHandler() {
        handleMap = new HashMap<>();
        handleTypes = Arrays.asList(HANDLE_TYPE.BEFORE_WRITE.name(), HANDLE_TYPE.AFTER_WRITE.name(), HANDLE_TYPE.ON_RESET.name(),
                HANDLE_TYPE.ON_TOGGLE.name(), HANDLE_TYPE.ON_TRIGGER.name(), HANDLE_TYPE.ON_RESULTS.name(), HANDLE_TYPE.WRITE_OVERRIDE.name(),
                HANDLE_TYPE.ON_MONITOR_CHANGE.name(), HANDLE_TYPE.PREREAD.name(), HANDLE_TYPE.VERIFY.name());
    }

    public ScriptHandler(ScriptEngine engine) {
        this();
        this.engine = engine;
    }

    public void setHandle(String handleType, ScriptObjectMirror func) {
        if (!handleTypes.contains(handleType.toUpperCase())) {
            log.warn("Cannot assign a script handler type {}", handleType);
            return;
        }
        if (func == null || !func.isFunction()) {
            log.warn("Cannot assign a script handler type {}.  Handler is not a function.", handleType);
            return;
        }
        try {
            handleMap.put(HANDLE_TYPE.valueOf(handleType.toUpperCase()), func);
        } catch (Exception e) {
            log.warn("Cannot assign a script handler type {}", handleType);
        }
    }

    public boolean has(HANDLE_TYPE type) {
        return handleMap.containsKey(type);
    }

    public void handle(HANDLE_TYPE type, Object ... data) {
        if (!has(type))
            return;
        ScriptObjectMirror func = handleMap.get(type);
        if (!func.isFunction()) {
            log.error("Could not call handler for cheat [{}].  It's not a function", type.name());
            return;
        }
        try {
            func.call(this, data);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void handle(HANDLE_TYPE type) {
        if (!has(type))
            return;
        ScriptObjectMirror func = handleMap.get(type);
        if (!func.isFunction()) {
            log.error("Could not call handler for cheat [{}].  It's not a function", type.name());
            return;
        }
        try {
            func.call(this);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public Object handleReturn(HANDLE_TYPE type, Object ... data) {
        if (!has(type))
            return null;

        ScriptObjectMirror func = handleMap.get(type);
        if (!func.isFunction()) {
            log.error("Could not call handler for cheat [{}].  It's not a function", type.name());
            return null;
        }
        try {
            return func.call(this, data);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public Object handleReturn(HANDLE_TYPE type) {
        if (!has(type))
            return null;

        ScriptObjectMirror func = handleMap.get(type);
        if (!func.isFunction()) {
            log.error("Could not call handler for cheat [{}].  It's not a function", type.name());
            return null;
        }
        try {
            return func.call(this);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }



}
