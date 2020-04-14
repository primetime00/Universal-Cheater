package script;

import com.sun.jna.Memory;
import util.FormatTools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Value {
    enum ValueType {
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        BYTES
    }

    private String originalValue;
    transient int iValue;
    transient long lValue;
    transient byte bValue;
    transient float fValue;
    transient double dValue;
    transient short sValue;
    transient String sString;
    transient byte[] bytes;

    private ValueType type;

    private Value() {
        iValue = 0;
        lValue = 0;
        bValue = 0;
        fValue = 0f;
        dValue = 0.0;
        sValue = 0;
        sString = "";
        bytes = null;
        originalValue = "";
    }

    private void parseValue(String val, boolean isString) {
        originalValue = val;
        if (isString) {
            sString = val;
            type = ValueType.STRING;
            return;
        }
        try {
            Double.parseDouble(val);
            if (val.contains(".")) {//float or double
                if (Double.parseDouble(val) > Float.MAX_VALUE) {
                    dValue = Double.parseDouble(val);
                    type = ValueType.DOUBLE;
                    return;
                }
                fValue = Float.parseFloat(val);
                type = ValueType.FLOAT;
                return;
            }
            //integer
            long v = Long.parseLong(val);
            if (v <= Byte.MAX_VALUE) {
                bValue = (byte) v;
                type = ValueType.BYTE;
                return;
            }
            if (v <= Short.MAX_VALUE) {
                sValue = (short) v;
                type = ValueType.SHORT;
                return;
            }
            if (v <= Integer.MAX_VALUE) {
                iValue = (int) v;
                type = ValueType.INT;
                return;
            }
            lValue = v;
            type = ValueType.LONG;
        }
        catch (NumberFormatException e) { //we couldn't parse it, so lets assume bytes
            parseBytes(val);
        }
    }

    private void parseBytes(String val) {
        bytes = FormatTools.stringToBytes(val);
        type = ValueType.BYTES;
    }

    public int size() {
        switch (type) {
            case BYTE:
                return 1;
            case SHORT:
                return 2;
            case INT:
                return 4;
            case LONG:
                return 8;
            case FLOAT:
                return 4;
            case DOUBLE:
                return 8;
            case STRING:
                return sString.length();
            case BYTES:
                return bytes.length;
        }
        return 0;
    }


    public static Value createValue(String val) {
        Value v = new Value();
        v.parseValue(val, false);
        return v;
    }

    public static Value createString(String val) {
        Value v = new Value();
        v.parseValue(val, true);
        return v;
    }

    public Memory getMemory() {
        int vSize = size();
        Memory mem = new Memory(vSize);
        ByteBuffer buf = ByteBuffer.allocate(vSize).order(ByteOrder.LITTLE_ENDIAN);
        switch (type) {
            case BYTE:
                buf.put(bValue);
                break;
            case SHORT:
                buf.asShortBuffer().put(sValue);
                break;
            case INT:
                buf.asIntBuffer().put(iValue);
                break;
            case LONG:
                buf.asLongBuffer().put(lValue);
                break;
            case FLOAT:
                buf.asFloatBuffer().put(fValue);
                break;
            case DOUBLE:
                buf.asDoubleBuffer().put(dValue);
                break;
            case STRING:
                buf.put(sString.getBytes());
                break;
            case BYTES:
                buf.put(bytes);
                break;
        }
        mem.write(0, buf.array(), 0, vSize);
        return mem;
    }

    @Override
    public String toString() {
        return originalValue;
    }
}
