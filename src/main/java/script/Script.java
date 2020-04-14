package script;

import cheat.Cheat;
import com.sun.jna.Memory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Script {
    private ScriptEngine engine;
    private String name;

    public Script(String file) throws Exception {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        File f = new File(file);
        if (!f.exists()) {
            throw new Exception("Could not find cheat script: " + file);
        }
        Bindings bindings = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("_script", this);
        engine.eval("var ScriptTools = Java.type(\'script.ScriptTools\');");
        engine.eval("var SearchList = Java.type(\'script.ArraySearchResultList\');");
        engine.eval("var Value = Java.type(\'script.Value\');");
        engine.eval("var AOBTools = Java.type(\'util.AOBTools\');");
        engine.eval("var FormatTools = Java.type(\'util.FormatTools\');");
        engine.eval("var Log = ScriptTools.log;");
        engine.eval("var createValue = Value.createValue;");
        engine.eval("var createString = Value.createString;");
        engine.eval("var createSearchList = function() {return new SearchList(_script);}");
        ScriptObjectMirror ctx = (ScriptObjectMirror) engine.eval(new FileReader(f));
        ScriptObjectMirror init = (ScriptObjectMirror) engine.getContext().getAttribute("initialize");
        if (init != null && init.isFunction()) {
            ((Invocable)engine).invokeFunction("initialize");
        }
        this.name = file;
    }


    public void search(long pos, Memory mem) throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror search = (ScriptObjectMirror) engine.getContext().getAttribute("search");
        if (search != null) {
            ((Invocable)engine).invokeFunction("search", pos, mem);
        }
    }

    public void write() throws ScriptException, NoSuchMethodException {
        ScriptObjectMirror apply = (ScriptObjectMirror) engine.getContext().getAttribute("apply");
        if (apply != null) {
            ((Invocable)engine).invokeFunction("apply");
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
}
