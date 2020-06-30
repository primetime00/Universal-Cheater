package io;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.Value;

import java.util.Collection;
import java.util.Objects;

public class Filter implements OperationProcessor{
    public static Logger log = LoggerFactory.getLogger(Filter.class);
    private int offset;
    private Value expect;
    private Value low;
    private Value high;
    private boolean complete;

    public Filter() {
        this(0, null);
    }

    public Filter(int offset, String expect) {
        this.offset = offset;
        this.expect = Value.createValue(expect);
        this.complete = false;
    }

    public Filter(int offset, String low, String high) {
        this.offset = offset;
        this.low = Value.createValue(low);
        this.high = Value.createValue(high);
        this.complete = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return offset == filter.offset &&
                Objects.equals(expect, filter.expect) &&
                Objects.equals(low, filter.low) &&
                Objects.equals(high, filter.high);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, expect, low, high);
    }

    public int getOffset() {
        return offset;
    }

    public Value getExpect() {
        return expect;
    }

    public Value getLow() {
        return low;
    }

    public Value getHigh() {
        return high;
    }

    @Override
    public void process(Collection<ArraySearchResult> results, long pos, Memory mem) {
        for (ArraySearchResult result: results) {
            if (expect != null) {
                byte[] bytes = result.getBytes(mem, offset, expect.size());
                if (!expect.equals(bytes)) {
                    result.setValid(false);
                }
            } else if (low != null && high != null) {
                byte[] bytes = result.getBytes(mem, offset, high.size());
                if (!Value.range(bytes, low, high)) {
                    result.setValid(false);
                }
            } else {
                log.error("Filter is not complete.  Results will be filtered");
                result.setValid(false);
            }
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void searchComplete(Collection<ArraySearchResult> results) {
        complete = true;
    }

    @Override
    public void readJson(JsonObject data, JsonDeserializationContext ctx) {
        this.offset = data.get("offset").getAsInt();
        this.expect = ctx.deserialize(data.get("expect"), Value.class);
        this.low = ctx.deserialize(data.get("low"), Value.class);
        this.high = ctx.deserialize(data.get("high"), Value.class);
    }

    @Override
    public void reset() {
        complete = false;
    }
}
