package util;

import com.sun.jna.Memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FormatTools {
    public static String bytesToString(byte [] bytes) {
        StringBuffer buf = new StringBuffer();
        for (byte b : bytes) {
            buf.append(String.format("%02X", b));
        }
        return buf.toString();
    }

    public static String valueToHex(long value) {
        return String.format("%X", value);
    }

    public static long memoryToValue(Memory mem, long position, int size) {
        ByteBuffer bb = ByteBuffer.wrap(mem.getByteArray(position, size)).order(ByteOrder.LITTLE_ENDIAN);
        long res = -1;
        switch (size) {
            default:
                res = Integer.toUnsignedLong(bb.getInt());
                break;
            case 1:
                res = Byte.toUnsignedLong(bb.get());
                break;
            case 2:
                res = Short.toUnsignedLong(bb.getShort());
                break;
            case 4:
                res = Integer.toUnsignedLong(bb.getInt());
                break;
            case 8:
                res = bb.getLong();
                break;
        }
        return res;
    }

    public static byte [] stringToBytes(String val) {
        val = val.trim().replace(" ", "");
        ByteBuffer bb = ByteBuffer.allocate(val.length()/2);
        for (int i=0; i<val.length(); i+=2) {
            byte b = (byte)(Short.parseShort(val.substring(i, i+2), 16) & 0xFF);
            bb.put(b);
        }
        return bb.rewind().array();
    }
}
