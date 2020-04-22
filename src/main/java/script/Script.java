package script;

import com.sun.jna.Memory;
import io.Cheat;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Script {
    private ScriptEngine engine;
    private StringBuffer scriptLog;
    private String name;

    public Script(String name, Reader reader) throws ScriptException, NoSuchMethodException {
        this.name = name;
        initialize(reader);
    }
    public Script(String file) throws Exception {
        File f = new File(file);
        if (!f.exists()) {
            throw new Exception("Could not find cheat script: " + file);
        }
        FileReader reader = new FileReader(f);
        this.name = file;
        initialize(reader);
    }

    public void log(String message) {
        if (scriptLog == null)
            scriptLog = new StringBuffer();
        scriptLog.append(message).append("\n");
    }

    public String getLog() {
        return scriptLog.toString();
    }

    private void initialize(Reader reader) throws ScriptException, NoSuchMethodException {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        Bindings bindings = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("_script", this);
        engine.eval("var Cheat = Java.type('io.Cheat');");
        engine.eval("var Code = Java.type('io.Code');");
        engine.eval("var Filter = Java.type('io.Filter');");
        engine.eval("var Detect = Java.type('io.Detect');");
        engine.eval("var cheatSearch = function(cheat, mem, base) {Java.type('util.SearchTools').search(cheat, mem, base);}");
        engine.eval("var writeCheat = function(cheat) { return Java.type('util.MemoryTools').writeCheat(cheat);}");
        engine.eval("var logMessage = function(msg) {_script.log(msg);}");
/*
        engine.eval("var ScriptTools = Java.type(\'script.ScriptTools\');");
        engine.eval("var SearchList = Java.type(\'script.ArraySearchResultList\');");
        engine.eval("var Value = Java.type(\'script.Value\');");
        engine.eval("var AOBTools = Java.type(\'util.AOBTools\');");
        engine.eval("var FormatTools = Java.type(\'util.FormatTools\');");
        engine.eval("var Log = ScriptTools.log;");
        engine.eval("var createValue = Value.createValue;");
        engine.eval("var createString = Value.createString;");
        engine.eval("var createSearchList = function() {return new SearchList(_script);}");*/
        ScriptObjectMirror ctx = (ScriptObjectMirror) engine.eval(reader);
        ScriptObjectMirror init = (ScriptObjectMirror) engine.getContext().getAttribute("initialize");
        if (init != null && init.isFunction()) {
            ((Invocable)engine).invokeFunction("initialize");
        }
    }


    public void search(long pos, Memory mem) throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror search = (ScriptObjectMirror) engine.getContext().getAttribute("search");
        if (search != null) {
            ((Invocable)engine).invokeFunction("search", mem, pos);
        }
    }

    public void write() throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror apply = (ScriptObjectMirror) engine.getContext().getAttribute("write");
        if (apply != null) {
            ((Invocable)engine).invokeFunction("write");
        }
    }

    public List<Cheat> getCheats() {
        ScriptObjectMirror cheatList = (ScriptObjectMirror) engine.getContext().getAttribute("getCheats");
        try {
            if (cheatList != null) {
                return (List<Cheat>) ((Invocable) engine).invokeFunction("getCheats");
            }
        } catch (Exception e) {
            ScriptTools.log.error("Could not get script cheats: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void searchComplete() throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror apply = (ScriptObjectMirror) engine.getContext().getAttribute("searchComplete");
        if (apply != null) {
            ((Invocable)engine).invokeFunction("searchComplete");
        }
    }

    public List<io.Cheat> getAllCheats() {
        List<io.Cheat> cheats = new ArrayList<>();
        for (Object value : getEngine().getContext().getBindings(ScriptContext.ENGINE_SCOPE).values()) {
            if (value instanceof io.Cheat) {
                cheats.add((io.Cheat) value);
            }
        }
        return cheats;
    }
}
