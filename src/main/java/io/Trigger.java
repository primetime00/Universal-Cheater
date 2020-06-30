package io;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Trigger {
    static public Logger log = LoggerFactory.getLogger(Trigger.class);
    public enum Type {
        BUTTON,
        KEYPRESS
    };

    public enum Behavior {
        TOGGLE,
        ONETIME,
        HOLD
    };

    protected int key;
    transient protected boolean on;
    transient protected Type type;
    transient protected Behavior behavior;

    public Trigger() {
        on = false;
    }

    public Trigger(Type type, Behavior behavior) {
        this(type, behavior, 0);
    }

    public Trigger(Type type, Behavior behavior, int key) {
        this();
        this.type = type;
        this.behavior = behavior;
        this.key = key;
        if (this.behavior == Behavior.TOGGLE)
            on = true;
    }

    public Trigger(String type, String behavior, int key) {
        this(stringToType(type, Type.BUTTON), stringToBehavior(behavior, Behavior.HOLD), key);
    }


    static Type stringToType(String type, Type def) {
        if (type.toLowerCase().equals("button"))
            return Type.BUTTON;
        else if (type.toLowerCase().equals("keypress"))
            return Type.KEYPRESS;
        return def;
    }

    static Behavior stringToBehavior(String behavior, Behavior def) {
        switch (behavior.toLowerCase()) {
            case "toggle":
                return Behavior.TOGGLE;
            case "onetime":
                return Behavior.ONETIME;
            case "hold":
                return Behavior.HOLD;
        }
        return def;
    }

    static String typeToString(Type type) {
        switch (type) {
            default:
            case BUTTON:
                return "button";
            case KEYPRESS:
                return "keypress";
        }
    }

    static String behaviorToString(Behavior behavior) {
        switch (behavior) {
            default:
            case TOGGLE:
                return "toggle";
            case ONETIME:
                return "onetime";
            case HOLD:
                return "hold";
        }
    }

    public void handle(TriggerInfo info) {
        if (info.getBehavior() == Behavior.HOLD) {
            if (info.isDown() && !on)
                on = true;
            else if (!info.isDown() && on)
                on = false;
        }
        else {
            on = !on;
            info.setDown(on);
        }

    }

    public void postHandle() {
        if (getBehavior() == Behavior.ONETIME) {
            if (on)
                on = false;
        }

    }

    public boolean isTriggered() {
        return on;
    }

    public int getKey() {
        return key;
    }

    public Type getType() {
        return type;
    }

    public Behavior getBehavior() {
        return behavior;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return key == trigger.key &&
                on == trigger.on &&
                type == trigger.type &&
                behavior == trigger.behavior;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, on, type, behavior);
    }

    static public class TriggerDeserializer implements JsonDeserializer<Trigger>, JsonSerializer<Trigger> {

        @Override
        public Trigger deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject val = jsonElement.getAsJsonObject();
            Type tType = Type.BUTTON;
            int key = 0;
            Behavior tBehavior = Behavior.TOGGLE;
            if (val.has("type")) {
                tType = Trigger.stringToType(val.get("type").getAsString(), tType);
            }
            if (val.has("behavior")) {
                tBehavior = Trigger.stringToBehavior(val.get("behavior").getAsString(), tBehavior);
            }
            if (tType == Type.KEYPRESS && val.has("key")) {
                key = val.get("key").getAsInt();
            }
            return new Trigger(tType, tBehavior, key);
        }

        @Override
        public JsonElement serialize(Trigger trigger, java.lang.reflect.Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject root = new JsonObject();
            root.addProperty("on", trigger.on);
            if (trigger.getType() == Type.KEYPRESS)
                root.addProperty("key", trigger.key);
            root.addProperty("type", Trigger.typeToString(trigger.getType()));
            root.addProperty("behavior", Trigger.behaviorToString(trigger.getBehavior()));
            return root;
        }
    }

    public static class TriggerInfo {
        private Behavior behavior;
        private int key;
        private boolean isDown;

        public TriggerInfo(Behavior behavior, int key, boolean isDown) {
            this.behavior = behavior;
            this.key = key;
            this.isDown = isDown;
        }

        public Behavior getBehavior() {
            return behavior;
        }

        public int getKey() {
            return key;
        }

        public boolean isDown() {
            return isDown;
        }

        public void setDown(boolean on) {
            isDown = on;
        }
    }
}
