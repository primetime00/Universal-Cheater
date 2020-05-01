package script;

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
        engine.eval("var CheatBuilder = Java.type('io.CheatBuilder');");
        engine.eval("var Code = Java.type('io.Code');");
        engine.eval("var CodeBuilder = Java.type('io.CodeBuilder');");
        engine.eval("var Filter = Java.type('io.Filter');");
        engine.eval("var Detect = Java.type('io.Detect');");
        engine.eval("var InvDetect = Java.type('io.InvDetect');");
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


    public ScriptEngine getEngine() {
        return engine;
    }

    public void initCheat(Cheat c) throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror apply = (ScriptObjectMirror) engine.getContext().getAttribute("initializeCheat");
        if (apply != null && apply.isFunction()) {
            ((Invocable)engine).invokeFunction("initializeCheat", c);
        }
    }

    public void searchComplete() throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror apply = (ScriptObjectMirror) engine.getContext().getAttribute("searchComplete");
        if (apply != null && apply.isFunction()) {
            ((Invocable)engine).invokeFunction("searchComplete");
        }
    }

    public void handleScriptCheatSuccess(Cheat cheat, int codesWritten, int totalCodes) throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror success = (ScriptObjectMirror) getEngine().getContext().getAttribute("cheatSuccess");
        if (success != null && success.isFunction()) {
            ((Invocable)getEngine()).invokeFunction("cheatSuccess", cheat, codesWritten, totalCodes);
        }
    }

    public void handleScriptCheatFailed(Cheat cheat) throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror success = (ScriptObjectMirror) getEngine().getContext().getAttribute("cheatFailed");
        if (success != null && success.isFunction()) {
            ((Invocable)getEngine()).invokeFunction("cheatFailed", cheat);
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
