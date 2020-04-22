package engine;

import cheat.AOB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.*;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.MemoryUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import script.ArraySearchResult;
import script.Value;
import util.MemoryTools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import static engine.Util.*;
import static org.junit.Assert.*;

public class ProcessTest {
    Process process;
    Gson gson;
    CheatFile cheatFile;
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
        Util.createCheats("cheatTest");
        BlockingQueue<Message> blk = new BlockingArrayQueue<>();
        int pid = (int) ProcessHandle.current().pid();
        process = new Process(pid, "cheatTest", "Cheat", "testcode.cht", blk);
    }

    @After
    public void tearDown() throws Exception {
        killStructure(data);
        killStructure(otherData);
        killStructure(lastData);
    }

    @Test
    public void testRead() {
        process.performSearch(); //gather initial results and filter
        CheatFile f = process.getCheatFile();
        assertEquals(3, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        assertEquals(4, c.getResults().getAllValidList().size());
        assertTrue(c.getResults().getAllValidList().get(0).isValid());
        assertEquals(2, c.getCodes().size());
        Code code = c.getCodes().get(0);
        ArraySearchResult res = c.getResults().getAllValidList().get(0);
        System.out.println(String.format("Found code at %X with value %d", code.getFirstOffset()+res.getAddress(), ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN).rewind().getInt()));
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.getInt();
        assertEquals(5, value);
        assertTrue(c.getCodes().get(0).getOperations().get(0) instanceof Filter);
        assertTrue(c.getCodes().get(0).getOperations().get(0).isComplete());
        assertTrue(c.getCodes().get(0).getOperations().get(1) instanceof Detect);
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        process.performSearch(); //this search will gather initial values for the detection
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        process.performSearch(); //haven't changed any values, still expect the operation to not be complete
        assertFalse(c.getCodes().get(0).getOperations().get(1).isComplete());
        changeFakeStructure(data, 10);
        process.performSearch(); //our value has changed, our operation should be complete
        assertTrue(c.getCodes().get(0).getOperations().get(1).isComplete());
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(10, value);
        assertTrue(f.getCheats().get(0).getResults().getAllValidList().size() > 0);
    }

    @Test
    public void testOperations() {
        killStructure(otherData);
        killStructure(lastData);

        process.performSearch();
        CheatFile f = process.getCheatFile();
        assertEquals(3, f.getCheats().size());
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

        changeFakeStructure(data, 10);
        process.performSearch();
        assertTrue(f.getCheats().get(0).getCodes().get(0).operationsComplete());
        assertTrue(f.getCheats().get(0).getCodes().get(1).operationsComplete());
    }

    @Test
    public void testDetect() {
        CheatFile f = process.getCheatFile();
        List<Filter> filters = f.getCheats().get(0).getCodes().get(0).getFilters();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(filters);
        process.performSearch();
        assertEquals(12, f.getCheats().get(0).getResults().getAllValidList().size());
        process.performSearch();
        assertEquals(12, f.getCheats().get(0).getResults().getAllValidList().size());
        changeFakeStructure(data, 10);
        process.performSearch();
        assertEquals(1, f.getCheats().get(0).getResults().getAllValidList().size());
    }

    @Test
    public void testWrite() {
        //remove filters and detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(3, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        assertEquals(4, c.getResults().getAllValidList().size());
        //verify the current value
        Code code = c.getCodes().get(0);
        Code code2 = c.getCodes().get(1);
        ArraySearchResult res = c.getResults().getAllValidList().get(0);
        ByteBuffer bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        long value = bb.getInt();
        assertEquals(5, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(24, value);


        //let's write the new value
        process.writeCheats();
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(9, value);
        bb = ByteBuffer.wrap(MemoryTools.readBytes(code2, res, 4)).order(ByteOrder.LITTLE_ENDIAN);
        value = bb.getInt();
        assertEquals(999, value);
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
    public void testVerify() {
        //remove filters and detects
        CheatFile f = process.getCheatFile();
        List<Detect> detects = f.getCheats().get(0).getCodes().get(0).getDetects();
        f.getCheats().get(0).getCodes().get(0).getOperations().removeAll(detects);
        process.performSearch(); //gather initial results and filter
        assertEquals(3, f.getCheats().size());
        Cheat c = f.getCheats().get(0);
        assertEquals(4, c.getResults().getAllValidList().size());
        //verify the current value
        Code code = c.getCodes().get(0);
        Code code2 = c.getCodes().get(1);
        ArraySearchResult res = c.getResults().getAllValidList().get(0);
        //let's write the new value
        process.writeCheats();
        assertEquals(4, c.getResults().getAllValidList().size());
        //next change the AOB and try to write cheats again!
        changeFakeStructureAOB(data);

        //try to write a second time with bad aob
        process.writeCheats();
        assertEquals(3, c.getResults().getAllValidList().size());

    }

}