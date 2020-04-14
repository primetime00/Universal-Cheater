package cheat;

public class Code {
    public enum Type {
        BYTE,
        INT,
        LONG,
        SHORT,
        FLOAT,
        DOUBLE
    };
    String value;
    int offset;
    Type type;
    transient int iValue;
    transient long lValue;
    transient byte bValue;
    transient float fValue;
    transient double dValue;
    transient short sValue;

    public Code(int offset, String val) {
        this.offset = offset;
        this.value = val;
        parseValue(val);
    }


    private void parseValue(String val) {
        Double.parseDouble(val);
        if (val.contains(".")) {//float or double
            if (Double.parseDouble(val) > Float.MAX_VALUE) {
                dValue = Double.parseDouble(val);
                type = Type.DOUBLE;
                return;
            }
            fValue = Float.parseFloat(val);
            type = Type.FLOAT;
            return;
        }
        //integer
        long v = Long.parseLong(val);
        if (v <= Byte.MAX_VALUE) {
            bValue = (byte) v;
            type = Type.BYTE;
            return;
        }
        if (v <= Short.MAX_VALUE) {
            sValue = (short) v;
            type = Type.SHORT;
            return;
        }
        if (v <= Integer.MAX_VALUE) {
            iValue = (int) v;
            type = Type.INT;
            return;
        }
        lValue = v;
        type = Type.LONG;
    }

}
