package io;

import com.google.gson.*;
import com.sun.jna.Memory;
import script.ArraySearchResultList;

import java.lang.reflect.Type;

public interface OperationProcessor {
    void process(ArraySearchResultList resultList, long pos, Memory mem);
    boolean isComplete();
    void searchComplete(ArraySearchResultList resultList);
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
                default:
                    return null;
            }
        }
    }

}
