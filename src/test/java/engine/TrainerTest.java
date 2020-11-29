package engine;

import com.google.gson.Gson;
import games.RunnableCheat;
import io.CheatFile;
import message.Message;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;

public class TrainerTest {

    Process process;
    Gson gson;
    CheatFile cheatFile;
    UnitCom unitCom = new UnitCom();
    UnitCom trainerCom = new UnitCom();
    BlockingQueue<Message> blk = new BlockingArrayQueue<>();

    @Before
    public void setUp() throws Exception {
        Process.debugMode = true;
        gson = CheatApplication.getGson();
        Util.setupKeyHandler();
        Util.deleteCheats("cheatTest");
        Util.createCheats("cheatTest");
        int pid = unitCom.runUnitApp();
        unitCom.connect(27015);
        Thread.sleep(100);
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "trainer.cht"), blk);
    }

    @After
    public void tearDown() throws Exception {
        unitCom.quit();
        Process.getInstance().exit();
        Thread.sleep(200);
    }

    @Test
    public void testTrainerLaunch() throws Exception {
        Thread.sleep(1000);
        Assert.assertTrue(new FindProcess().exists("Trainer.exe"));
        trainerCom.connect(27016);
        trainerCom.quit();
        Thread.sleep(1000);
        Assert.assertFalse(new FindProcess().exists("Trainer.exe"));
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "trainer.cht"), blk);
        Thread.sleep(1000);
        trainerCom.connect(27016);
        Assert.assertEquals(1, new FindProcess().getProcesses("Trainer.exe").size());
        process = Process.create(new RunnableCheat("cheatTest", "Cheat", "trainer.cht"), blk);
        Thread.sleep(1000);
        Assert.assertEquals(1, new FindProcess().getProcesses("Trainer.exe").size());
        trainerCom.quit();
    }


}
