package engine;

import com.google.gson.Gson;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import games.RunnableCheat;
import io.*;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import script.ArraySearchResult;
import script.Script;
import util.MemoryTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ProcessTest {
    Process process;
    Gson gson;
    CheatFile cheatFile;
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
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "testcode.cht"), blk);
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
    public void testRead() throws IOException {
        process.performSearch(); //gather initial results and filter
        CheatFile f = process.getCheatFile();
        assertEquals(4, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        List<ArraySearchResult> validList = getValidResults(c);
        assertEquals(1, validList.size());
        assertTrue(validList.get(0).isValid());
        assertEquals(2, c.getCodes().size());
        Code code = c.getCodes().get(0);
        ArraySearchResult res = validList.get(0);
        System.out.println(String.format("Found code at %X with value %d", code.getFirstOffset()+res.getAddress(), ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN).rewind().getInt()));
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.get();
        assertEquals(10, value);
        assertTrue(c.getCodes().get(0).getOperations().get(0) instanceof Filter);
        assertTrue(c.getCodes().get(0).getOperations().get(0).isComplete());
        assertTrue(c.getCodes().get(0).getOperations().get(1) instanceof Detect);
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        process.performSearch(); //this search will gather initial values for the detection
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        process.performSearch(); //haven't changed any values, still expect the operation to not be complete
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        unitCom.changeTestValue((byte)1, (byte)15);
        process.performSearch(); //our value has changed, our operation should be complete
        assertTrue(c.getCodes().get(0).getOperations().get(1).isComplete());
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.get();
        assertEquals(15, value);
        assertTrue(f.getCheats().get(0).getResults().size() > 0);
    }

    @Test
    public void testOperations() throws IOException {
        process.performSearch();
        CheatFile f = process.getCheatFile();
        assertEquals(4, f.getCheats().size());
        assertEquals(2, f.getCheats().get(0).getCodes().size());
        assertTrue(f.getCheats().get(0).hasOperations());
        assertTrue(f.getCheats().get(0).getCodes().get(0).hasOperations());
        assertFalse(f.getCheats().get(0).getCodes().get(1).hasOperations());
        Cheat c = f.getCheats().get(0);
        assertFalse(f.getCheats().get(0).getCodes().get(0).operationsComplete());
        assertTrue(f.getCheats().get(0).getCodes().get(1).operationsComplete());

        process.performSearch();
        assertFalse(f.getCheats().get(0).getCodes().get(0).operationsComplete());
        assertTrue(f.getCheats().get(0).getCodes().get(1).operationsComplete());

        unitCom.changeTestValue((byte)1, (byte)15);
        process.performSearch();
        assertTrue(f.getCheats().get(0).getCodes().get(0).operationsComplete());
        assertTrue(f.getCheats().get(0).getCodes().get(1).operationsComplete());
    }

    @Test
    public void testDetect() throws IOException {
        CheatFile f = process.getCheatFile();
        List<Filter> filters = f.getCheats().get(0).getCodes().get(0).getFilters();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(filters);
        process.performSearch();
        assertEquals(3, getValidResults(f.getCheats().get(0)).size());
        process.performSearch();
        assertEquals(3, getValidResults(f.getCheats().get(0)).size());
        assertEquals(0, f.getCheats().get(0).getResults().size() );
        unitCom.changeTestValue((byte)1, (byte)16);
        process.performSearch();
        assertEquals(1, f.getCheats().get(0).getResults().size() );

    }

    @Test
    public void testWrite() {
        //remove detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(4, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        //verify the current value
        Code code = c.getCodes().get(0);
        Code code2 = c.getCodes().get(1);
        ArraySearchResult res = c.getResults().iterator().next();
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.get();
        assertEquals(10, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getShort();
        assertEquals(20000, value);


        //let's write the new value
        process.writeCheats();
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.get();
        assertEquals(50, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getShort();
        assertEquals(999, value);
    }

    @Test
    public void testWriteOnce() {
        //remove detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(4, f.getCheats().size());
        assertEquals(3, f.getCheats().get(1).getResults().size());
        assertEquals(3, f.getCheats().get(2).getResults().size());
        process.writeCheats();
        assertEquals(0, f.getCheats().get(1).getResults().size());
        assertEquals(3, f.getCheats().get(2).getResults().size());
    }


    @Test
    public void testCheatScript() {
        //remove detects
        CheatFile f = process.getCheatFile();
        Cheat cheat = f.getCheats().get(2);
        assertNotNull(cheat.getScriptHandler());
        Script s = cheat.getScriptHandler().getScript();
        assertNotNull(s);
        assertEquals(1, countLog(s, "Cheat Initialized"));
        process.performSearch();
        assertEquals(0, countLog(s, "Before Write"));
        assertEquals(0, countLog(s, "After Write"));
        process.writeCheats();
        assertEquals(1, countLog(s, "Before Write"));
        assertEquals(1, countLog(s, "After Write"));
    }

    @Test
    public void testNoOp() {
        System.out.println("Starting");
        //remove filters and detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        List<Filter> filters = f.getCheats().get(0).getCodes().get(0).getFilters();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(filters);
        process.performSearch(); //gather initial results and filter
        process.performSearch();
    }


    @Test
    public void testVerify() throws IOException {
        //remove filters and detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(4, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        //verify the current value
        Code code = c.getCodes().get(0);
        Code code2 = c.getCodes().get(1);
        //let's write the new value
        process.writeCheats();
        assertEquals(1, c.getResults().size());
        //next change the AOB and try to write cheats again!
        unitCom.aobChange((byte)1);

        //try to write a second time with bad aob
        process.writeCheats();
        assertEquals(0, getValidResults(c).size());
        assertEquals(0, c.getResults().size());

    }

    @Test
    public void testAbsolute() throws IOException {
        CheatFile f = process.getCheatFile();
        List <Cheat> badCheat = f.getCheats().stream().filter(e -> !e.getName().contains("Absolute")).collect(Collectors.toList());
        f.getCheats().removeAll(badCheat);
        process.performSearch();
        assertEquals(1, f.getCheats().size());
    }

    @Test
    public void testMemoryProtection() {
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(4, f.getCheats().size());
        Cheat c = f.getCheats().get(0);

        //get First result and modify it's memory page protection
        ArraySearchResult res = c.getResults().iterator().next();
        long addr = res.getAddress();
        WinDef.DWORDByReference result = new WinDef.DWORDByReference();
        int success = MemoryProtection.INSTANCE.VirtualProtectEx(Process.getHandle(), new Pointer(addr), 4, Kernel32.PAGE_READWRITE, result);
        assertEquals(1, success);

    }

    private int countLog(Script s, String test) {
        return (int) Arrays.stream(s.getLog().split("\n")).filter(e->e.equals(test)).count();
    }


}