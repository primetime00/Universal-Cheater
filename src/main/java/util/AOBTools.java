package util;

import cheat.AOB;

public class AOBTools {

    public static AOB createAOB(String aob) {
        return new AOB(aob);
    }

    public static short[] parseAOB(String aob) {
        aob = aob.toUpperCase();
        String bts[] = aob.split(" ");
        short res[] = new short[bts.length];
        for (int i=0; i<bts.length; ++i) {
            if (bts[i].equals("??")) {
                res[i] = Short.MAX_VALUE;
            } else {
                res[i] = (short) (Integer.parseInt(bts[i], 16) & 0xFF);
            }
        }
        return res;
    }

    public static String displayAOBCompare(AOB aob, byte [] bytes) {
        StringBuffer buf = new StringBuffer();
        int index = 0;
        for (short s : aob.getAob()) {
            if (s > 0xFF) {
                buf.append("?? ");
            }
            else {
                if (index >= bytes.length)
                    break;
                if (s == (short)bytes[index]) {
                    buf.append(String.format("%02X ", s));
                }
                else {
                    buf.append("xx ");
                }
            }
            index++;
        }
        return buf.toString();
    }

    public static String displayAOB(AOB aob) {
        StringBuffer buf = new StringBuffer();
        for (short s : aob.getAob()) {
            if (s > 0xFF) {
                buf.append("?? ");
            }
            else {
                buf.append(String.format("%02X ", s));
            }
        }
        return buf.toString();
    }
}
