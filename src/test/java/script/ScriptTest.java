package script;

import cheat.AOB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import engine.Process;
import engine.Util;
import io.*;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Before;
import org.junit.Test;
import util.MemoryTools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static engine.Util.*;
import static org.junit.Assert.*;

public class ScriptTest {
    Process process;
    Gson gson;
    static byte[] data = createFakeStructure((short) 1);
    static byte[] otherData = createFakeStructure((short) 99);
    static byte[] lastData = createFakeStructure((short) 33);



    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Value.class, new Value.ValueDeserializer())
                .registerTypeAdapter(AOB.class, new AOB.AOBDeserializer())
                .registerTypeAdapter(OperationProcessor.class, new OperationProcessor.AOBDeserializer())
                .create();
        Util.deleteCheats("cheatTest");
        Util.createCheats("cheatTest");
        BlockingQueue<Message> blk = new BlockingArrayQueue<>();
        int pid = (int) ProcessHandle.current().pid();
        process = new Process(pid, "cheatTest", "Cheat", "scripting.cht", blk);

    }

    @Test
    public void testScript() {
        assertEquals(1, process.getScriptList().size());
        Script s = process.getScriptList().get(0);
        assertEquals(1, countLog(s, "initialize"));
        assertEquals(0, countLog(s, "search"));
        process.performSearch(); //gather initial results and filter
        CheatFile f = process.getCheatFile();
        assertEquals(2, countLog(s, "search"));
        assertEquals(1, countLog(s, "search complete"));
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("cheat1");
        Cheat cheat2 = (Cheat) s.getEngine().getContext().getAttribute("cheat2");
        Cheat cheat3 = (Cheat) s.getEngine().getContext().getAttribute("cheat3");
        assertFalse(cheat1.operationsComplete());
        assertTrue(cheat2.operationsComplete());
        assertTrue(cheat3.operationsComplete());
        process.writeCheats();
        process.performSearch();
        assertFalse(cheat1.operationsComplete());
        changeFakeStructure(data, 10);
        process.performSearch();
        assertTrue(cheat1.operationsComplete());

    }

    @Test
    public void testWrite() {
        Script s = process.getScriptList().get(0);
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("cheat1");

        process.performSearch();
        Code code = cheat1.getCodes().get(0);
        Code code2 = cheat1.getCodes().get(1);
        ArraySearchResult res = cheat1.getResults().getAllValidList().get(0);
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.getInt();
        assertEquals(5, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(24, value);
        process.writeCheats();
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(5, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(999, value);

        cheat1.verify();
        process.performSearch();
        changeFakeStructure(data, 10);
        process.performSearch();

        process.writeCheats();
        res = cheat1.getResults().getAllValidList().get(0);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(9, value);
    }

    @Test
    public void testVerify() throws InterruptedException {
        //remove filters and detects
        killStructure(otherData);
        killStructure(lastData);
        Script s = process.getScriptList().get(0);
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("cheat1");
        List<Detect> detects = cheat1.getCodes().get(0).getDetects();
        cheat1.getCodes().get(0).getOperations().removeAll(detects);
        List<Filter> filters = cheat1.getCodes().get(0).getFilters();
        cheat1.getCodes().get(0).getOperations().removeAll(filters);

        process.performSearch(); //gather initial results and filter

        //verify the current value
        Code code = cheat1.getCodes().get(0);
        Code code2 = cheat1.getCodes().get(1);
        ArraySearchResult res = cheat1.getResults().getAllValidList().get(0);
        int initialSize = cheat1.getResults().getAllValidList().size();

        //let's write the new value
        process.writeCheats();
        //next change the AOB and try to write cheats again!
        changeFakeStructureAOB(data);

        //try to write a second time with bad aob
        process.writeCheats();
        assertEquals(initialSize-1, cheat1.getResults().getAllValidList().size());

    }

    private int countLog(Script s, String test) {
        return (int) Arrays.stream(s.getLog().split("\n")).filter(e->e.equals(test)).count();
    }
}