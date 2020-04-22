package engine;

import cheat.AOB;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import games.RunnableCheat;
import io.Cheat;
import io.CheatFile;
import io.Code;
import message.Message;
import message.ProcessComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.Script;
import util.MemoryTools;
import util.SearchTools;

import java.io.FileReader;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class Process {
    static Logger log = LoggerFactory.getLogger(Process.class);
    private int pid;
    private ReentrantLock lock;
    private static WinNT.HANDLE handle = null;
    CheatFile cheatFile;
    List<io.Cheat> cheatList;
    List<Script> scriptList;
    private List<Long> regionSize;
    private boolean closed;
    private Thread searchThread;
    private Thread writeThread;
    int cheatIndex;
    private RunnableCheat data;
    private String gameName;
    private final BlockingQueue<Message> messageQueue;
    private Set<Long> regionSet;
    private int passes;
    private Map<AOB, Set<io.Cheat>> scanMap;

    public Process(RunnableCheat data, BlockingQueue<Message> messageQueue) throws Exception {
        this.data = data;
        this.lock = new ReentrantLock();
        this.scanMap = new HashMap<>();
        cheatFile = readFile(CheatApplication.cheatDir, data.getSystem(), data.getCht());
        this.gameName = cheatFile.getGame();
        this.pid = getProcessID(cheatFile.getWindow());
        this.cheatList = cheatFile.getCheats();
        this.scriptList = createScripts(CheatApplication.cheatDir, data.getSystem());
        this.messageQueue = messageQueue;
        this.regionSize = cheatFile.getRegionSize();
        this.regionSet = new HashSet<>();
        this.passes = 0;
        openProcess();
        this.closed = false;
    }

    public Process(int pid, String directory, String system, String cht, BlockingQueue<Message> messageQueue) throws Exception {
        this.data = null;
        this.lock = new ReentrantLock();
        this.scanMap = new HashMap<>();
        cheatFile = readFile(directory, system, cht);
        this.gameName = cheatFile.getGame();
        this.pid = pid;
        this.cheatList = cheatFile.getCheats();
        this.scriptList = createScripts(directory, system);
        this.messageQueue = messageQueue;
        this.regionSize = cheatFile.getRegionSize();
        this.regionSet = new HashSet<>();
        this.passes = 0;
        openProcess();
        this.closed = false;
    }

    private List<Script> createScripts(String cheatDir, String system) {
        List<Script> scripts = new ArrayList<>();
        if (cheatFile.getScripts() != null) {
            cheatFile.getScripts().forEach(e -> {
                try {
                    Script s = new Script(String.format("%s/%s/%s", cheatDir, system, e));
                    scripts.add(s);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            });
        }
        return scripts;
    }

    private CheatFile readFile(String cheatDir, String system, String cht) throws Exception {

        try (FileReader fr = new FileReader(String.format("%s/%s/%s", cheatDir, system, cht))) {
            CheatFile fileData = CheatApplication.getGson().fromJson(fr, CheatFile.class);
            createScanMap(fileData);
            return fileData;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void createScanMap(CheatFile fileData) {
        scanMap.clear();
        if (fileData.getCheats() != null) {
            for (io.Cheat cheat : fileData.getCheats()) {
                AOB scan = cheat.getScan();
                if (!scanMap.containsKey(scan)) {
                    scanMap.put(scan, new HashSet<>());
                }
                Set<io.Cheat> cheatSet = scanMap.get(scan);
                cheatSet.add(cheat);
            }
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

    public void performSearch() {
        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        WinBase.SYSTEM_INFO info =  new WinBase.SYSTEM_INFO();
        long pos = 0;
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ((mbi.protect.intValue() & PAGE_READWRITE) != 0) {
                if (regionSize.size() == 0 || regionSize.stream().anyMatch(size -> size == mbi.regionSize.longValue())) {
                    if (passes == 0 || regionSet.contains(pos)) { //this is a new region for some reason, let's ignore it.
                        Memory mem = new Memory(mbi.regionSize.longValue());
                        Kernel32.INSTANCE.ReadProcessMemory(handle, new Pointer(pos), mem, mbi.regionSize.intValue(), null);
                        cheatSearch(pos, mem);
                        regionSet.add(pos);
                        log.trace("Searching...");
                    }
                }
            }
            pos += mbi.regionSize.longValue();
        }
        passes++;
        searchComplete();
    }

    private void searchComplete() {
        int count = 0;
        if (cheatList != null) {
            for (io.Cheat cheat : cheatList) {
                count += cheat.getResults().size();
                for (Code code : cheat.getCodes()) {
                    if (code.getOperations() != null && code.getOperations().size() > 0) {
                        code.getCurrentOperation().searchComplete(cheat.getResults());
                    }
                }
            }
        }
        if (scriptList != null) {
            for (Script script : scriptList) {
                try {
                    for (Cheat cheat : script.getAllCheats()) {
                        count += cheat.getResults().size();
                        for (Code code : cheat.getCodes()) {
                            if (code.getOperations() != null && code.getOperations().size() > 0) {
                                code.getCurrentOperation().searchComplete(cheat.getResults());
                            }
                        }
                    }
                    script.searchComplete();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        log.trace("Total results: {}", count);
    }

    private void cheatSearch(long pos, Memory mem) {
        scanMap.entrySet().forEach(entry -> {
            Set<io.Cheat> cheats = entry.getValue();
            if (cheats.stream().noneMatch(cheat -> cheat.getResults().getValidList(pos).size() == 0 || !cheat.operationsComplete())) { //we'll do not need to scan this set
                return;
            }
            //scan one time for this entire set;
            log.debug("Scanning for {}", cheats.iterator().next().getName());
            List<ArraySearchResult> results = SearchTools.aobSearch(cheats.iterator().next().getScan(), pos, mem);
            cheats.forEach(cheat -> cheat.getResults().addAll(results, pos));
        });
        if (cheatList != null) {
            for (io.Cheat cheat : cheatList) {
                if (cheat.hasOperations() && !cheat.operationsComplete()) {
                    cheat.getCodes().forEach(e -> e.processOperations(cheat.getResults(), pos, mem));
                }
            }
        }

        scriptList.forEach((script) -> {
            try {
                script.search(pos, mem);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        });
    }

    private void writeCheat() {
        if (cheatList != null) {
            cheatList.forEach(cheat -> {
                if (!cheat.hasWritableCode())
                    return;
                if (cheat.verify()) {
                    cheat.getCodes().forEach(c -> {
                        if (c.operationsComplete())
                            MemoryTools.writeCode(c, cheat.getResults().getAllValidList());
                    });
                }
            });
        }
        if (scriptList != null) {
            scriptList.forEach((script) -> {
                try {
                    script.write();
                } catch (Exception e) {
                    log.error("Could not write cheats: {}", e.getMessage());
                }
            });
        }
    }

    public void writeCheats() {
        writeCheat();
    }

    public void searchLoop() {
        int ret = 0;
        IntByReference ref = new IntByReference(ret);
        Kernel32.INSTANCE.GetExitCodeProcess(handle, ref);
        if (ref.getValue() != Kernel32.STILL_ACTIVE) { //the process has closed
            log.warn("The process has terminated!");
            close();
            closed = true;
        }
        //do we need to search ?
        if (scriptList.size() > 0 || cheatList.size() > 0) {
            try {
                lock.lock();
                performSearch();
            }
            finally {
                lock.unlock();
            }
        }
    }

    public void start() {
        searchThread = new Thread(this::searchProcess);
        writeThread = new Thread(this::writeProcess);
        searchThread.start();
        writeThread.start();
    }

    private void processWrites() {
        int ret = 0;
        IntByReference ref = new IntByReference(ret);
        log.trace("Writing...");
        Kernel32.INSTANCE.GetExitCodeProcess(handle, ref);
        if (ref.getValue() != Kernel32.STILL_ACTIVE) { //the process has closed
            log.warn("The process has terminated!");
            close();
            closed = true;
        }
        try {
            lock.lock();
            writeCheats();
        } finally {
            lock.unlock();
        }
    }

    public void writeProcess() {
        while (!closed) {
            try {
                processWrites();
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void searchProcess() {
        while (!closed) {
            try {
                searchLoop();
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
        io.Cheat cheat = findCheat(id);
        cheat.toggle();
    }

    public void resetCheat(int id) throws Exception {
        io.Cheat cheat = findCheat(id);
        cheat.reset();
    }


    private io.Cheat findCheat(int id) throws Exception {
        if (cheatList != null) {
            for (io.Cheat cheat : cheatList) {
                if (cheat.getId() == id)
                    return cheat;
            }
        }
        if (scriptList != null) {
            for (Script script: scriptList) {
                for (Cheat cheat: script.getAllCheats()) {
                    if (cheat.getId() == id)
                        return cheat;
                }
            }
        }
        return null;
        /*
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

         */
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

    public CheatFile getCheatFile() {
        return cheatFile;
    }

    public List<Script> getScriptList() {
        return scriptList;
    }
}
