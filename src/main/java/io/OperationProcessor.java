package io;

import com.google.gson.*;
import com.sun.jna.Memory;
import script.ArraySearchResult;
import script.ArraySearchResultList;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public interface OperationProcessor {
    void process(Collection<ArraySearchResult> results, long pos, Memory mem);
    boolean isComplete();
    void searchComplete(Collection<ArraySearchResult> result);
    void readJson(JsonObject data, JsonDeserializationContext ctx);
    void reset();


    class AOBDeserializer implements JsonDeserializer<OperationProcessor> {
        @Override
        public OperationProcessor deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject val = jsonElement.getAsJsonObject();
            String opType = val.get("type").getAsString();
            switch (opType) {
                case "filter":
                    Filter f = new Filter();
                    f.readJson(val.get("data").getAsJsonObject(), jsonDeserializationContext);
                    return f;
                case "detect":
                    Detect d = new Detect();
                    d.readJson(val.get("data").getAsJsonObject(), jsonDeserializationContext);
                    return d;
                case "invdetect":
                    InvDetect i = new InvDetect();
                    i.readJson(val.get("data").getAsJsonObject(), jsonDeserializationContext);
                    return i;
                case "instance":
                    Instance ins = new Instance();
                    ins.readJson(val.get("data").getAsJsonObject(), jsonDeserializationContext);
                    return ins;
                default:
                    return null;
            }
        }
    }

}
