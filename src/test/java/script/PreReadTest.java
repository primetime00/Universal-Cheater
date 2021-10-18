package script;

import com.google.gson.Gson;
import engine.Process;
import engine.*;
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
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PreReadTest {
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
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "readCheck.cht"), blk);

    }

    @After
    public void tearDown() throws Exception {
        unitCom.quit();
        Process.getInstance().exit();
        Thread.sleep(200);
    }

    private List<ArraySearchResult> getValidResults(Cheat c) {
        return ScanMap.get().getAllSearchResults(c).stream().filter(ArraySearchResult::isValid).collect(Collectors.toList());
    }

    @Test
    public void testScript() throws IOException {
        assertEquals(1, process.getScriptList().size());
        Script s = process.getScriptList().get(0);
        Cheat cheat1 = (Cheat) s.getEngine().getContext().getAttribute("readCheck");
        assertEquals(1, countLog(s, "Initialize"));
        assertEquals(0, countLog(s, "Cheat success"));
        process.performSearch(); //gather initial results and filter
        assertEquals(1, countLog(s, "Search Complete"));

        Code code = cheat1.getCodes().get(0);
        Code code2 = cheat1.getCodes().get(1);
        ArraySearchResult res = cheat1.getResults().iterator().next();
        process.writeCheats();
        assertEquals(1, countLog(s, "PreRead"));
        assertEquals(3, countLog(s, "2PreRead"));
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        short value = bb.getShort();
        assertEquals(10, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long v = bb.getShort();
        assertEquals(9, v);
    }

    private int countLog(Script s, String test) {
        return (int) Arrays.stream(s.getLog().split("\n")).filter(e->e.equals(test)).count();
    }
}