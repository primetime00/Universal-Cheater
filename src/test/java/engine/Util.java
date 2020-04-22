package engine;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public class Util {
    static public void createCheats(String name) {
        LocalResources res = new LocalResources();
        res.addDirectory("Cheat");
        res.addFile("Cheat", "cheat.cht");
        res.addFile("Cheat", "cheat2.cht");
        res.addFile("Cheat", "scripting.cht");
        res.addFile("Cheat", "testcode.cht");
        res.addFile("Cheat/scripts", "script.js");
        res.addFile("Cheat/scripts", "testscript.js");
        res.process(name);
    }

    static public void deleteCheats(String name) {
        try {
            Files.walk(Path.of(new File(name).getAbsolutePath()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {

        }
    }

    public static byte [] createFakeStructure(short id) {
        byte bytes[] = new byte[50];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).rewind();
        bb.putInt(40);
        bb.putInt(50);
        bb.putInt(60);
        bb.put((byte) 0x46);
        bb.put((byte) 0x41);
        bb.put((byte) 0x4b);
        bb.put((byte) 0x45);
        bb.put((byte)0);
        bb.putShort(id);
        bb.putInt(5);
        bb.putInt(24);
        bb.put((byte) 100);
        return bb.rewind().order(ByteOrder.LITTLE_ENDIAN).array();
    }

    public static void killStructure(byte[] data) {
        Arrays.fill(data, (byte) 0);
    }

    public static void changeFakeStructureAOB(byte[] data) {
        data[13] = 0;
    }


    public static void changeFakeStructure(byte[] data, int val) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(19, val);
    }


}
