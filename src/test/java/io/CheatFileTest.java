package io;

import cheat.AOB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.Memory;
import engine.CheatApplication;
import engine.Process;
import engine.UnitCom;
import engine.Util;
import games.RunnableCheat;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import script.Value;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CheatFileTest {
    Gson gson;
    UnitCom unitCom = new UnitCom();
    Process process;

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
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "cheat.cht"), blk);

    }

    @After
    public void tearDown() throws Exception {
        unitCom.quit();
        Process.getInstance().exit();
        Thread.sleep(200);
    }

    @Test
    public void readCheatFile() throws IOException {
        //URL url = getClass().getResource("/cheat_codes/Cheat/cheat.cht");
        //CheatFile f = gson.fromJson(new InputStreamReader(url.openStream()), CheatFile.class);
        CheatFile f = Process.getInstance().getCheatFile();
        Assert.assertEquals("Test Game", f.getGame());
        Assert.assertEquals("UnitTestApp.exe", f.getWindow().getWindowTitle());
        Assert.assertEquals(5, f.getCheats().size());
        Cheat c1 = f.getCheats().get(0);
        Cheat c2 = f.getCheats().get(1);
        Cheat c3 = f.getCheats().get(2);
        Cheat c4 = f.getCheats().get(3);
        Cheat c5 = f.getCheats().get(4);
        Assert.assertEquals("Cheat Operation", c1.getName());
        Assert.assertEquals("Cheat No Op", c2.getName());
        Assert.assertEquals("Cheat Both", c3.getName());
        assertEquals(1, c1.getCodes().size());
        assertEquals(1, c1.getCodes().get(0).getDetects().size());
        assertEquals(1, c1.getCodes().get(0).getFilters().size());

        assertEquals(2, c2.getCodes().size());
        assertEquals(0, c2.getCodes().get(0).getDetects().size());
        assertEquals(0, c2.getCodes().get(0).getFilters().size());

        assertEquals(2, c3.getCodes().size());
        assertEquals(0, c3.getCodes().get(0).getDetects().size());
        assertEquals(0, c3.getCodes().get(0).getFilters().size());
        assertEquals(1, c3.getCodes().get(1).getDetects().size());
        assertEquals(0, c3.getCodes().get(1).getFilters().size());


        Detect detect = c1.getCodes().get(0).getDetects().get(0);
        Filter filter = c1.getCodes().get(0).getFilters().get(0);
        assertEquals(122, detect.getOffset());
        assertEquals(122, filter.getOffset());
        Value v1 = detect.getMax();
        Memory mem = v1.getMemory();
        assertEquals(5, mem.getByte(0));
        Value v2 = detect.getMin();
        mem = v2.getMemory();
        assertEquals(3, mem.getByte(0));
        mem = filter.getExpect().getMemory();
        assertEquals(100, mem.getByte(0));
        assertEquals(3, c3.getCodes().get(0).getOffsets().size());

        assertNotNull(c4.getTrigger());
        assertEquals(Trigger.Type.BUTTON, c4.getTrigger().getType());
        assertEquals(Trigger.Behavior.ONETIME, c4.getTrigger().getBehavior());

        assertEquals("inc.js", c5.getScriptFile());
        assertNotNull(c5.getScriptHandler());

    }
}