package cheat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import script.Value;

import java.lang.reflect.Type;
import java.util.Arrays;

import static util.AOBTools.parseAOB;

public class AOB {
    private short []aob;
    private int startIndex;
    private int endIndex;

    public AOB(String aobN) {
        this.aob = parseAOB(aobN);
        this.startIndex = 0;
        for (int i=0; i<this.aob.length; ++i) {
            if (aob[i] != Short.MAX_VALUE && aob[i] != 0) {
                this.startIndex = i;
                break;
            }
        }
        this.endIndex = this.aob.length-1;
        for (int i=this.aob.length-1; i>=0; --i) {
            if (aob[i] != Short.MAX_VALUE && aob[i] != 0) {
                this.endIndex = i;
                break;
            }
        }
    }

    public short[] getAob() {
        return aob;
    }

    public short aobAtStart() {
        return aob[startIndex];
    }

    public short aobAtEnd() {
        return aob[endIndex];
    }

    public short aobAt(int index) {
        return aob[index];
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int size() {
        return aob.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AOB aob1 = (AOB) o;
        return Arrays.equals(aob, aob1.aob);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(aob);
    }

    static public class AOBDeserializer implements JsonDeserializer<AOB> {

        @Override
        public AOB deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String val = jsonElement.getAsString();
            return new AOB(val);
        }
    }

}
