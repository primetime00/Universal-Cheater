package script;

import com.google.common.collect.Lists;
import engine.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FormatTools;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.*;
import java.util.stream.Collectors;

public class ArraySearchResultList {
    static Logger log = LoggerFactory.getLogger(ArraySearchResultList.class);
    private Map<Long, Set<ArraySearchResult>> results;
    private Object scriptData = null;
    private ScriptEngine engine;
    private Script script;
    private boolean locked;

    public ArraySearchResultList(Script s) {
        this.script = s;
        this.engine = s.getEngine();
        results = new HashMap<>();
        this.locked = false;
        try {
            this.scriptData = s.getEngine().eval("(function() { return {} })(this)");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    public ArraySearchResultList() {
        results = new HashMap<>();
        this.script = null;
        this.engine = null;
    }

    public void add(ArraySearchResult result, long base) {
        if (locked)
            return;
        Set<ArraySearchResult> rList = results.get(base);
        if (rList == null) {
            rList = new HashSet<>();
            log.debug("Adding new base {}", FormatTools.valueToHex(base));
            results.put(base, rList);
        }
        rList.add(result);
        if (result.getScriptData() == null && script != null) {
            try {
                result.setScriptData(script.getEngine().eval("(function(){return {}})(this)"));
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
    }

    public void addAll(Collection<ArraySearchResult> results, long base) {
        for (ArraySearchResult res: results) {
            add(res, base);
        }
    }



    public List<ArraySearchResult> getList(long base) {
        if (!results.containsKey(base))
            return new ArrayList<>();
        return Lists.newArrayList(results.get(base));
    }

    public List<ArraySearchResult> getValidList(long base) {
        List<ArraySearchResult> res = getList(base);
        return res.stream().filter(e->e.isValid()).collect(Collectors.toList());
    }

    public List<ArraySearchResult> getAllValidList() {
        List<ArraySearchResult> res = new ArrayList<>();
        results.forEach((key, value) -> res.addAll(getValidList(key)));
        return res;
    }

    public void clear() {
        if (this.scriptData != null) {
            try {
                this.scriptData = engine.eval("(function() { return {} })(this)");
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
        results.clear();
    }

    public void remove(ArraySearchResult res) {
        results.entrySet().forEach(e->e.getValue().remove(res));
    }


    public Object getScriptData() {
        return scriptData;
    }

    public void setScriptData(Object scriptData) {
        this.scriptData = scriptData;
    }

    public int size() {
        return results.size();
    }

    public void lock() {
        locked = true;
    }
}
