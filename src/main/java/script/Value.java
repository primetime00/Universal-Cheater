package script;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.sun.jna.Memory;
import org.apache.commons.compress.utils.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FormatTools;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.Arrays;

public class Value implements Serializable {
    public static Logger log = LoggerFactory.getLogger(Value.class);

    enum ValueType {
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BYTES
    }

    private String originalValue;
    transient BigDecimal decimal;
    transient byte[] bytes;

    private ValueType type;
    private int valueSize;

    private Value() {
        bytes = null;
        originalValue = "";
        valueSize = 0;
        decimal = BigDecimal.valueOf(0);
    }
    private void parseValue(String val) throws ParseException {
        // "d 200.00 s 4"
        // "d 200 s 2"
        // "h 1A s 4
        // "b DEADBEEF"
        // "s HELLO WORLD"
        if (val == null)
            throw new ParseException("Could not parse a null string", 0);
        val = val.trim();
        originalValue = val;
        String type = String.valueOf(val.charAt(0)).toLowerCase();
        switch (type) {
            case "d":
                parseDecimal(val.substring(1));
                break;
            case "h":
                parseHexadecimal(val.substring(1));
                break;
            case "b":
                parseBytes(val.substring(1));
                break;
            case "s":
                parseString(val.substring(1));
                break;
            default:
                //attempt to parse as default number
                parseDecimal(val);
                break;
        }
    }

    private void parseString(String val) {
        bytes = val.getBytes();
        type = ValueType.BYTES;
        calculateSize();
    }

    private void parseHexadecimal(String val) throws ParseException {
        val = val.trim();
        int pos = val.indexOf('s');
        if (pos < 0) {//no size specified
            parseHex(val, 0);
        } else {
            parseHex(val.substring(0, pos - 1), parseSize(val.substring(pos + 1)));
        }
        if (valueSize <= 0)
            throw new ParseException("Could not parse/calculate value size", 0);
    }

    private void parseHex(String val, int sz) throws ParseException {
        //assume integer
        try {
            long v = Long.parseLong(val, 16);
            parseNumber(String.valueOf(v), sz);
        } catch (Exception e) {
            throw  new ParseException(String.format("Could not parse hex %s", val),0);
        }
    }

    private void parseDecimal(String val) throws ParseException {
        val = val.trim().toLowerCase();
        int pos = val.indexOf('s');
        if (pos < 0) {//no size specified
                parseNumber(val, 0);
        } else {
            parseNumber(val.substring(0, pos - 1), parseSize(val.substring(pos + 1)));
        }
        if (valueSize <= 0)
            throw new ParseException("Could not parse/calculate value size", 0);
    }

    private int parseSize(String sz) throws ParseException {
        sz = sz.trim();
        try {
            return Integer.parseInt(sz);
        } catch (Exception e) {
            throw new ParseException(String.format("Could not parse value size %s, must be 1,2,4,8", sz), 0);
        }
    }

    private void calculateSize() {
        valueSize = -1;
        switch (type) {
            case BYTE:
                valueSize = 1;
                break;
            case SHORT:
                valueSize = 2;
                break;
            case INT:
                valueSize = 4;
                break;
            case LONG:
                valueSize = 8;
                break;
            case FLOAT:
                valueSize = 4;
                break;
            case DOUBLE:
                valueSize = 8;
                break;
            case BYTES:
                valueSize = bytes.length;
                break;
        }
    }

    private void parseNumber(String val, int sz) throws ParseException {
        switch (sz) {
            case 0:
                if (val.contains(".")) {//float or double
                    decimal = BigDecimal.valueOf(Double.parseDouble(val));
                    if (decimal.doubleValue() > Float.MAX_VALUE)
                        type = ValueType.DOUBLE;
                    else
                        type = ValueType.FLOAT;
                }
                else {
                    decimal = new BigDecimal(val);
                    if (decimal.longValue() <= 0xFF)
                        type = ValueType.BYTE;
                    else if (decimal.longValue() <= 0xFFFF)
                        type = ValueType.SHORT;
                    else if (decimal.longValue() <= 0xFFFFFFFFL)
                        type = ValueType.INT;
                    else
                        type = ValueType.LONG;
                }
                break;
            case 1:
                if (val.contains("."))
                    throw new ParseException(String.format("Cannot store float/double into 1 byte %s", originalValue), 0);
                decimal = new BigDecimal(val);
                if (decimal.longValue() > 0xFF)
                    throw new ParseException(String.format("Cannot store value into 1 byte %s", originalValue), 0);
                type = ValueType.BYTE;
                break;
            case 2:
                if (val.contains("."))
                    throw new ParseException(String.format("Cannot store float/double into 1 byte %s", originalValue), 0);
                decimal = new BigDecimal(val);
                if (decimal.longValue() > 0xFFFF)
                    throw new ParseException(String.format("Cannot store value into 1 byte %s", originalValue), 0);
                type = ValueType.SHORT;
                break;
            case 4:
                decimal = new BigDecimal(val);
                if (!val.contains(".")) {
                    if (decimal.longValue() > 0xFFFFFFFFL)
                        throw new ParseException(String.format("Cannot store value into 4 byte %s", originalValue), 0);
                    type = ValueType.INT;
                }
                else {
                    type = ValueType.FLOAT;
                }
                break;
            case 8:
                decimal = new BigDecimal(val);
                if (!val.contains(".")) {
                    type = ValueType.LONG;
                }
                else {
                    type = ValueType.DOUBLE;
                }
                break;
            default:
                throw new ParseException(String.format("Cannot parse %s into value", originalValue), 0);
        }
        calculateSize();
    }

    private void parseBytes(String val) {
        bytes = FormatTools.stringToBytes(val);
        type = ValueType.BYTES;
        calculateSize();
    }

    public int size() {
        return valueSize;
    }

    public static Value createValue(String val) {
        Value v = new Value();
        try {
            if (val != null)
                v.parseValue(val);
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return v;
    }

    public static int compare(Value v1, Value v2) throws Exception {
        if (v1.type == ValueType.BYTES || v2.type == ValueType.BYTES)
            throw new ParseException("Cannot compare string of bytes", 0);
        return v1.decimal.compareTo(v2.decimal);
    }

    public Memory getMemory() {
        int vSize = size();
        Memory mem = new Memory(vSize);
        ByteBuffer buf = ByteBuffer.allocate(vSize).order(ByteOrder.LITTLE_ENDIAN);
        if (type == ValueType.BYTES) {
            buf.put(bytes);
        }
        else {
            switch (vSize) {
                case 1:
                    buf.put(decimal.byteValue());
                    break;
                case 2:
                    buf.asShortBuffer().put(decimal.shortValue());
                    break;
                case 4:
                    if (type == ValueType.FLOAT)
                        buf.asFloatBuffer().put(decimal.floatValue());
                    else
                        buf.asIntBuffer().put(decimal.intValue());
                    break;
                case 8:
                    if (type == ValueType.DOUBLE)
                        buf.asDoubleBuffer().put(decimal.doubleValue());
                    else
                        buf.asLongBuffer().put(decimal.longValue());
                    break;
                default:
                    log.error("Cannot get memory for size {}", vSize);
                    buf.put((byte) 0);
                    break;
            }
        }
        mem.write(0, buf.order(ByteOrder.LITTLE_ENDIAN).array(), 0, vSize);
        return mem;
    }

    @Override
    public String toString() {
        return originalValue;
    }

    static public class ValueDeserializer implements JsonDeserializer<Value> {

        @Override
        public Value deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String val = jsonElement.getAsString();
            Value v = new Value();
            try {
                v.parseValue(val);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return v;
        }
    }

    public BigDecimal getDecimal() {
        return decimal;
    }

    public boolean equals(byte[] bytes) {
        if (bytes.length != size())
            return false;
        return Arrays.equals(bytes, getMemory().getByteArray(0, bytes.length));
    }

    public static boolean range(byte[] bytes, Value low, Value high) {
        try {
            long val = ByteUtils.fromLittleEndian(bytes);
            return val <= high.getDecimal().longValue() && val >= low.getDecimal().longValue();
        } catch (Exception e) {
            log.error("Could not convert bytes to big integer!");
            return false;
        }
    }


}
