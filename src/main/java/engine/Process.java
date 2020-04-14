package engine;

import cheat.Cheat;
import cheat.Code;
import cheat.MasterCode;
import cheat.StaticCheat;
import com.google.gson.Gson;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import games.RunnableCheat;
import io.CheatFile;
import message.Message;
import message.ProcessComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.Script;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class Process implements Runnable {
    static Logger log = LoggerFactory.getLogger(Process.class);
    private int pid;
    private ReentrantLock lock;
    private static WinNT.HANDLE handle = null;
    //Map<String, MasterCode> masterList;
    List<MasterCode> masterList;
    List<Script> scriptList;
    private boolean closed;
    private Thread searchThread;
    private Thread writeThread;
    int cheatIndex;
    private RunnableCheat data;
    private List<Long> regionSize;
    private String gameName;
    private final BlockingQueue<Message> messageQueue;

    public Process(RunnableCheat data, BlockingQueue<Message> messageQueue) throws Exception {
        this.data = data;
        this.lock = new ReentrantLock();
        CheatFile file = readFile(data.getSystem(), data.getCht());
        this.gameName = file.getGame();
        this.pid = getProcessID(file.getWindow());
        this.masterList = new ArrayList<>();
        this.scriptList = new ArrayList<>();
        this.messageQueue = messageQueue;
        this.regionSize = new ArrayList<>();
        openProcess();
        applyCheatFile(file);
        this.closed = false;
    }

    private void applyCheatFile(CheatFile file) {
        cheatIndex = 1;
        log.debug("Applying cheat file {}", file.getGame());
        this.regionSize = file.getRegionSize();
        if (file.getMasterCodes() != null) {
            file.getMasterCodes().forEach(e -> {
                MasterCode ct = new MasterCode(e.getMaster(), e.isResearchAfterFound(), e.getSearch(), e.getChange());
                e.getCheats().forEach(f -> {
                    StaticCheat cheat = new StaticCheat(ct, f.getId(), f.getName());
                    ct.addCheat(cheat);
                    f.getCodes().forEach(code -> {
                        cheat.addCode(new Code(code.getOffset(), code.getValue()));
                    });
                });
                masterList.add(ct);
            });
        }
        if (file.getScripts() != null) {
            file.getScripts().forEach(e -> {
                try {
                    Script s = new Script(String.format("%s/%s/%s", CheatApplication.cheatDir, data.getSystem(), e));
                    scriptList.add(s);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            });
        }
    }

    private CheatFile readFile(String system, String cht) throws Exception {
        Gson gson = new Gson();
        try (FileReader fr = new FileReader(String.format("%s/%s/%s", CheatApplication.cheatDir, system, cht))) {
            CheatFile fileData = gson.fromJson(fr, CheatFile.class);
            return fileData;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private int getProcessID(CheatFile.Window window) throws Exception {
        char title[] = new char[1024];
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(window.getWindowClass(), null);
        if (hwnd != null) {
            User32.INSTANCE.GetWindowText(hwnd, title, 1024);
            if (window.isPartialMatch()) {
                if (new String(title).contains(window.getWindowTitle())) {
                    IntByReference ref = new IntByReference();
                    User32.INSTANCE.GetWindowThreadProcessId(hwnd, ref);
                    return ref.getValue();
                }
            } else {
                if (new String(title).equals(window.getWindowTitle())) {
                    IntByReference ref = new IntByReference();
                    User32.INSTANCE.GetWindowThreadProcessId(hwnd, ref);
                    return ref.getValue();
                }
            }
        }
        throw new Exception("Could not find window!");
    }

    public void exit() {
        closed = true;
        if (writeThread != null) {
            try {
                writeThread.join();
                writeThread = null;
            } catch (InterruptedException e) {
                log.warn("Thread exit interrupted.");
            }
        }
        if (searchThread != null) {
            try {
                searchThread.join();
                searchThread = null;
            } catch (InterruptedException e) {
                log.warn("Thread exit interrupted.");
            }
        }
        close();
    }


    public void close() {
        if (handle != null) {
            Kernel32.INSTANCE.CloseHandle(handle);
            handle = null;
        }
    }

    private void openProcess() {
        handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, pid);
    }

    public void search() {
        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        WinBase.SYSTEM_INFO info =  new WinBase.SYSTEM_INFO();
        long pos = 0;
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ((mbi.protect.intValue() & PAGE_READWRITE) != 0) {
                if (regionSize.size() == 0 || regionSize.stream().anyMatch(size -> size == mbi.regionSize.longValue())) {
                    Memory mem = new Memory(mbi.regionSize.longValue());
                    Kernel32.INSTANCE.ReadProcessMemory(handle, new Pointer(pos), mem, mbi.regionSize.intValue(), null);
                    searchMemory(pos, mem, mbi.regionSize.longValue());
                    log.trace("Searching...");
                }
            }
            pos += mbi.regionSize.longValue();
        }
    }

    private void searchMemory(long pos, Memory mem, long size) {
        masterList.forEach((value) -> {
            if (!value.isOld()) {//i'm not old.  I can still function
                log.trace("I don't need to search since I'm not old.");
                return;
            }
            value.search(pos, mem);
        });
        scriptList.forEach((script) -> {
            try {
                script.search(pos, mem);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        });
    }

    private void writeCheat() {
        masterList.forEach((value) -> value.write(handle));
        scriptList.forEach((value) -> {
            try {
                value.write();
            } catch (Exception e) {
                log.error("Could not write cheats: {}", e.getMessage());
            }
        });
    }

    public void writeCheats() {
        writeCheat();
    }

    public void process() {
        try {
            lock.lock();
            int ret = 0;
            IntByReference ref = new IntByReference(ret);
            log.trace("Searching...");
            Kernel32.INSTANCE.GetExitCodeProcess(handle, ref);
            if (ref.getValue() != Kernel32.STILL_ACTIVE) { //the process has closed
                log.warn("The process has terminated!");
                close();
                closed = true;
            }
            if (scriptList.size() > 0 || masterList.stream().anyMatch(MasterCode::isOld)) {
                search();
            }
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        searchThread = new Thread(this);
        writeThread = new Thread(() -> {
            while (!closed) {
                try {
                    processWrites();
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        searchThread.start();
        writeThread.start();
    }

    private void processWrites() {
        try {
            lock.lock();
            int ret = 0;
            IntByReference ref = new IntByReference(ret);
            log.trace("Writing...");
            Kernel32.INSTANCE.GetExitCodeProcess(handle, ref);
            if (ref.getValue() != Kernel32.STILL_ACTIVE) { //the process has closed
                log.warn("The process has terminated!");
                close();
                closed = true;
            }
            writeCheats();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        while (!closed) {
            try {
                process();
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn("Search thread interrupted!");
                break;
            } catch (Exception e) {
                log.error(e.getMessage());
                break;
            }
        }
        messageQueue.add(new Message(new ProcessComplete(true)));
        log.info("Search and apply thread exited");
    }

    public void toggleCheat(int id) throws Exception {
        Cheat code = findCheat(id);
        code.toggle();
    }

    public void resetCheat(int id) throws Exception {
        Cheat code = findCheat(id);
        code.reset();
    }

    private MasterCode findMaster(int id) throws Exception {
        for (MasterCode c : masterList) {
            for (Cheat item : c.getCodes()) {
                if (item.getId() == id)
                    return c;
            }
        }
        throw new Exception(String.format("Could not find master code containing cheat with id %d", id));
    }

    private Cheat findCheat(int id) throws Exception {
        for (MasterCode c : masterList) {
            for (Cheat item : c.getCodes()) {
                if (item.getId() == id)
                    return item;
            }
        }
        List<Cheat> scriptCheats = new ArrayList<>();
        scriptList.forEach(s -> scriptCheats.addAll(s.getCheats()));
        for (Cheat item: scriptCheats) {
            if (item.getId() == id) {
                return item;
            }
        }
        throw new Exception(String.format("Could not find cheat with id %d", id));
    }

    public RunnableCheat getData() {
        return data;
    }

    public String getGameName() {
        return gameName;
    }

    public static WinNT.HANDLE getHandle() {
        return handle;
    }
}
