package script;

import com.google.gson.Gson;
import engine.CheatApplication;
import engine.Process;
import engine.UnitCom;
import engine.Util;
import games.RunnableCheat;
import io.*;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.MemoryTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.*;

public class ScriptTest {
    Process process;
    Gson gson;
    UnitCom unitCom = new UnitCom();

    @Before
    public void setUp() throws Exception {
        Process.debugMode = true;
        gson = CheatApplication.getGson();
        Util.setupKeyHandler();
        Util.deleteCheats("cheatTest");
        Util.createCheats("cheatTest");
        BlockingQueue<Message> blk = new BlockingArrayQueue<>();
        int pid = unitCom.runUnitApp();
        unitCom.connect(27015);
        Thread.sleep(100);
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "scripting.cht"), blk);

    }

    @After
    public void tearDown() throws Exception {
        unitCom.quit();
        Process.getInstance().exit();
        Thread.sleep(200);
    }

    @Test
    public void testScript() throws IOException {
        assertEquals(1, process.getScriptList().size());
        Script s = process.getScriptList().get(0);
        assertEquals(1, countLog(s, "Initialize"));
        assertEquals(0, countLog(s, "Cheat success"));
        process.performSearch(); //gather initial results and filter
        CheatFile f = process.getCheatFile();
        assertEquals(1, countLog(s, "Search Complete"));
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("cheat1");
        Cheat cheat2 = (Cheat) s.getEngine().getContext().getAttribute("cheat2");
        Cheat cheat3 = (Cheat) s.getEngine().getContext().getAttribute("cheat3");
        assertFalse(cheat1.operationsComplete());
        assertTrue(cheat2.operationsComplete());
        assertTrue(cheat3.operationsComplete());
        process.writeCheats();
        assertEquals(3, countLog(s, "Cheat success"));
        process.performSearch();
        assertFalse(cheat1.operationsComplete());
        unitCom.changeTestValue((byte)1, (byte)15);
        process.performSearch();
        assertTrue(cheat1.operationsComplete());

    }

    @Test
    public void testWrite() throws IOException {
        Script s = process.getScriptList().get(0);
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("cheat1");

        process.performSearch();
        Code code = cheat1.getCodes().get(0);
        Code code2 = cheat1.getCodes().get(1);
        ArraySearchResult res = cheat1.getResults().getAllValidList().get(0);
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.get();
        assertEquals(10, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getShort();
        assertEquals(20000, value);
        process.writeCheats();
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.get();
        assertEquals(10, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getShort();
        assertEquals(999, value);

        cheat1.verify();
        process.performSearch();
        unitCom.changeTestValue((byte)1, (byte)15);
        process.performSearch();

        process.writeCheats();
        res = cheat1.getResults().getAllValidList().get(0);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.get();
        assertEquals(9, value);
    }

    @Test
    public void testVerify() throws InterruptedException, IOException {
        //remove filters and detects
        unitCom.killStructure((byte)2);
        unitCom.killStructure((byte)3);
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
        unitCom.aobChange((byte)1);

        //try to write a second time with bad aob
        process.writeCheats();
        //assertEquals(initialSize-1, cheat1.getResults().getAllValidList().size());
        assertEquals(1, countLog(s, "Cheat failed"));

    }

    private int countLog(Script s, String test) {
        return (int) Arrays.stream(s.getLog().split("\n")).filter(e->e.equals(test)).count();
    }
}