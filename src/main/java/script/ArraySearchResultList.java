package script;

import com.google.common.collect.Lists;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.*;
import java.util.stream.Collectors;

public class ArraySearchResultList {
    private Map<Long, Set<ArraySearchResult>> results;
    private Object scriptData = null;
    private ScriptEngine engine;
    private Script script;

    public ArraySearchResultList(Script s) {
        this.script = s;
        this.engine = s.getEngine();
        results = new HashMap<>();
        try {
            this.scriptData = s.getEngine().eval("(function() { return {} })(this)");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    public void add(ArraySearchResult result, long base) {
        Set<ArraySearchResult> rList = results.get(base);
        if (rList == null) {
            rList = new HashSet<>();
            results.put(base, rList);
        }
        rList.add(result);
        if (result.getScriptData() == null) {
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
        try {
            this.scriptData = engine.eval("(function() { return {} })(this)");
        } catch (ScriptException e) {
            e.printStackTrace();
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
}
