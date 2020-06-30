package io;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;

import java.lang.reflect.Type;
import java.util.*;

public class Code {
    static Logger log = LoggerFactory.getLogger(Code.class);
    private List<OVPair> offsets;
    protected List<OperationProcessor> operations;
    transient protected int currentProcessor;

    public Code() {
        this.currentProcessor = -1;
        this.offsets = new ArrayList<>();
        this.operations = new ArrayList<>();
    }

    public Code(int offset, String value) {
        this();
        this.offsets.add(new OVPair(offset, value));
    }

    public Code(int offset, String value, OperationProcessor ... ops) {
        this(offset, value);
        if (ops != null && ops.length > 0) {
            Collections.addAll(this.operations, ops);
            this.currentProcessor = 0;
        }
    }

    public Code(List<OVPair> offsets, List<OperationProcessor> operations) {
        this.offsets = offsets;
        this.operations = operations;
        if (this.operations != null && this.operations.size() > 0)
            this.currentProcessor = 0;
        else
            this.currentProcessor = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Code code = (Code) o;
        return  Objects.equals(offsets, code.offsets) &&
                Objects.equals(operations, code.operations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsets, operations);
    }

    public List<OVPair> getOffsets() {
        return offsets;
    }

    public List<OperationProcessor> getOperations() {
        return operations;
    }

    public List<Filter> getFilters() {
        List<Filter> filters = new ArrayList<>();
        if (operations != null)
            operations.stream().filter(e -> e instanceof Filter).forEach(f -> filters.add((Filter) f));
        return filters;
    }

    public List<Detect> getDetects() {
        List<Detect> detects = new ArrayList<>();
        if (operations != null)
            operations.stream().filter(e -> e instanceof Detect).forEach(f -> detects.add((Detect) f));
        return detects;
    }

    public boolean hasOperations() {
        return operations != null && operations.size() > 0;
    }

    public boolean hasOperations(Class<? extends OperationProcessor> opClass) {
        if (hasOperations()) {
            return operations.stream().anyMatch(opClass::isInstance);
        }
        return false;
    }

    public List<OperationProcessor> getFilterList(Class<? extends OperationProcessor> opClass) {
        List<OperationProcessor> ops = new ArrayList<>();
        if (hasOperations()) {
            for (OperationProcessor op : operations) {
                if (opClass.isInstance(op))
                    ops.add(op);
            }
        }
        return ops;
    }


    public boolean operationsComplete() {
        if (!hasOperations())
            return true;
        return getOperations().get(getOperations().size()-1).isComplete();
    }


    public void processOperations(Collection<ArraySearchResult> results, long pos, Memory mem) {
        if (getOperations() == null || getOperations().size() == 0 || currentProcessor >= getOperations().size())
            return;
        if (getOperations().get(currentProcessor).isComplete()) {
            currentProcessor++;
        }
        getOperations().get(currentProcessor).process(results, pos, mem);
    }

    public OperationProcessor getCurrentOperation() {
        return operations.get(currentProcessor);
    }

    public void addOperation(OperationProcessor op) {
        if (operations.size() == 0)
            currentProcessor = 0;
        operations.add(op);
    }

    public void addOffset(int offset, String value) {
        offsets.add(new OVPair(offset, value));
    }

    public OVPair findOffsetValue(int offset) {
        for (OVPair item: offsets) {
            if (item.getOffset() == offset)
                return item;
        }
        return null;
    }

    public void reset() {
        if (operations == null || operations.size() == 0) {
            currentProcessor = -1;
        }
        else {
            currentProcessor = 0;
            operations.forEach(OperationProcessor::reset);
        }

    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Code:");
        for (OVPair pair : offsets) {
            buf.append(pair.toString());
        }
        return buf.toString();
    }

    public String toAbsoluteAddress(long addr) {
        StringBuffer buf = new StringBuffer();
        buf.append("Code:");
        for (OVPair pair : offsets) {
            buf.append(pair.getAddress(addr));
        }
        return buf.toString();
    }


    static public class CodeDeserializer implements JsonDeserializer<Code> {

        @Override
        public Code deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject val = jsonElement.getAsJsonObject();
            boolean hasOffset = val.has("offset");
            boolean hasValue = val.has("value");
            Code code = new Code();
            if (val.has("offsets")) {
                code.offsets = jsonDeserializationContext.deserialize(val.get("offsets"), new TypeToken<ArrayList<OVPair>>(){}.getType());
            }
            if (val.has("operations")) {
                code.operations = jsonDeserializationContext.deserialize(val.get("operations"), new TypeToken<ArrayList<OperationProcessor>>(){}.getType());
            }
            if (hasOffset && !hasValue) { //we need a value, default to 0
                code.addOffset(val.get("offset").getAsInt(), "0");
            }
            else if (!hasOffset && hasValue) { //we need a offset, default to 0
                code.addOffset(0, val.get("value").getAsString());
            }
            else if (hasOffset) { //we have both a single offset/value
                code.addOffset(val.get("offset").getAsInt(), val.get("value").getAsString());
            }
            if (code.getOperations().size() > 0)
                code.currentProcessor = 0;
            return code;
        }
    }

    public OVPair getFirstOffsetPair() {
        if (offsets.size() == 0)
            return new OVPair(0, "0");
        return offsets.get(0);
    }

    public int getFirstOffset() {
        if (offsets.size() == 0)
            return 0;
        return offsets.get(0).getOffset();
    }

}
