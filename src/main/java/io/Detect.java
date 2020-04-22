package io;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;
import script.Value;

import java.util.Objects;

public class Detect implements OperationProcessor {
    static Logger log = LoggerFactory.getLogger(Detect.class);
    private int offset;
    private Value min;
    private Value max;
    private int size;
    private boolean complete;

    static public class DetectData {
        enum DetectRange {EQUAL, INRANGE, OUTRANGE};
        long readValue;
        DetectRange detectRange;
        int round;

        public DetectData(long readValue) {
            this.readValue = readValue;
            this.round = 0;
            this.detectRange = DetectRange.EQUAL;
        }

        public long getReadValue() {
            return readValue;
        }

        public DetectRange getDetectRange() {
            return detectRange;
        }

        public int getRound() {
            return round;
        }

        public void incrementRound() {
            round+=1;
        }

        public void process(long val, long max, long min) {
            if (round > 0 && readValue != val) {//there was a change
                long diff = Math.abs(readValue - val);
                if (diff <= max && diff >= min) {
                    detectRange = DetectRange.INRANGE;
                }
                else {
                    detectRange = DetectRange.OUTRANGE;
                }
            }
        }
    }

    public Detect() {
        this(0, null, null, 0);
    }

    public Detect(int offset, String min, String max) {
        this(offset, min, max, 0);
        this.offset = offset;
        if (min != null && max != null) {
            this.min = Value.createValue(min);
            this.max = Value.createValue(max);
        }
        this.complete = false;

    }

    public Detect(int offset, String min, String max, int size) {
        this.offset = offset;
        if (min != null && max != null) {
            this.min = Value.createValue(min);
            this.max = Value.createValue(max);
        }
        this.size = size;
        this.complete = false;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Detect detect = (Detect) o;
        return offset == detect.offset &&
                size == detect.size &&
                complete == detect.complete &&
                Objects.equals(min, detect.min) &&
                Objects.equals(max, detect.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, min, max, size, complete);
    }

    public int getOffset() {
        return offset;
    }

    public Value getMin() {
        return min;
    }

    public Value getMax() {
        return max;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void process(ArraySearchResultList resultList, long pos, Memory mem) {
        for (ArraySearchResult res: resultList.getValidList(pos)) {
            if (res.getMiscData() == null || !(res.getMiscData() instanceof DetectData)) {
                res.setMiscData(new DetectData(res.getBytesValue(mem, offset, size == 0 ? max.size() : size)));
            }
            DetectData detectData = (DetectData) res.getMiscData();
            detectData.process(res.readValue(getOffset(), size == 0 ? max.size() : size), max.value(), min.value());
            if (detectData.getDetectRange() == DetectData.DetectRange.OUTRANGE)
                res.setValid(false);
            detectData.incrementRound();
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void searchComplete(ArraySearchResultList resultList) {
        boolean found = resultList.getAllValidList().stream().anyMatch(e ->
                e.getMiscData() != null &&
                        e.getMiscData() instanceof DetectData &&
                        ((DetectData) e.getMiscData()).getDetectRange() == DetectData.DetectRange.INRANGE);
        if (found) {
            for (ArraySearchResult res : resultList.getAllValidList()) {
                if (res.getMiscData() instanceof DetectData) {
                    res.setValid(((DetectData)res.getMiscData()).getDetectRange() == DetectData.DetectRange.INRANGE);
                }
            }
        }
        complete = found && resultList.getAllValidList().size() > 0;
    }

    @Override
    public void readJson(JsonObject data, JsonDeserializationContext ctx) {
        this.offset = data.get("offset").getAsInt();
        this.max = ctx.deserialize(data.get("max"), Value.class);
        this.min = ctx.deserialize(data.get("min"), Value.class);
        if (data.has("size"))
            this.size = ctx.deserialize(data.get("size"), Integer.class);
        else
            this.size = max.size();
    }

    @Override
    public void reset() {
        complete = false;
    }

}
