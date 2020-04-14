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

}
