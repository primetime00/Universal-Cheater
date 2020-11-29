package engine;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import games.Game;
import games.RunnableCheat;
import io.*;
import message.Message;
import message.ProcessComplete;
import org.jnativehook.GlobalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.Script;
import util.FormatTools;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static com.sun.jna.platform.win32.WinNT.PAGE_EXECUTE_READWRITE;
import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class Process {
    static Logger log = LoggerFactory.getLogger(Process.class);
    static private Process instance = null;
    static public boolean debugMode = false;
    private long pid;
    private static WinNT.HANDLE handle = null;
    private WinNT.HWND selectedHWND;
    CheatFile cheatFile;
    List<io.Cheat> cheatList;
    List<Script> scriptList;
    private List<RegionSizeRange> regionSize;
    private boolean closed;
    private Thread searchThread;
    private Thread writeThread;
    private Thread trainerThread;
    private RunnableCheat data;
    private String gameName;
    private final BlockingQueue<Message> messageQueue;
    private Set<Long> regionSet;
    private int passes;
    private ScanMap scanMap;
    private KeyHandler keyHandler;
    private Memory readMemory;


    public Process(BlockingQueue<Message> messageQueue) throws Exception {
        this.scanMap = ScanMap.get();
        this.messageQueue = messageQueue;
        this.regionSet = new HashSet<>();
        this.passes = 0;
        this.closed = false;
        instance = this;
    }

    public Process(RunnableCheat data, BlockingQueue<Message> messageQueue) throws Exception {
        this(messageQueue);
        this.data = data;
        this.cheatFile = readFile(data);
        this.pid = new FindProcess().getProcess(cheatFile.getProcess()).getPid();
        openProcess();
        this.gameName = cheatFile.getGame();
        this.cheatList = cheatFile.getCheats();
        this.scriptList = createScripts(data);
        this.regionSize = cheatFile.getRegionSize();
        openTrainer();
        keyHandler = new KeyHandler();
        if (!debugMode) {
            GlobalScreen.addNativeKeyListener(keyHandler);
            this.scanMap.addUpdateHandler(() -> keyHandler.update(scanMap));
        }
        this.scanMap.update(this.cheatList, this.scriptList);
        if (cheatList != null)
            this.cheatList.stream().filter(e->e.getScriptHandler() != null).forEach(c -> c.getScriptHandler().initialize(c));

    }

    private void openTrainer() {
        Trainer trainer = this.cheatFile.getTrainer();
        if (trainer == null)
            return;
        try {
            //is the trainer already running?
            String trainerApp = trainer.findTrainerApp(getGameDirectory(this.data));
            FindProcess fp = new FindProcess();
            if (fp.exists(trainerApp))
                return;
            String [] cmd = {"cmd.exe", "/c", Paths.get(trainerApp).toAbsolutePath().toString() };
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Process create(RunnableCheat data, BlockingQueue<Message> messageQueue) throws Exception {
        if (instance != null) {
            log.warn("You are opening a new process without closing the old");
            instance.exit();
        }
        try {
            instance = new Process(data, messageQueue);
        } catch (Exception e) {
            instance = null;
            throw e;
        }
        return instance;
    }



    private List<Script> createScripts(RunnableCheat data) {
        List<Script> scripts = new ArrayList<>();
        if (cheatFile.getScripts() != null) {
            cheatFile.getScripts().forEach(e -> {
                try {
                    Script s = new Script(String.format("%s/%s/scripts/%s", data.getDirectory(), data.getSystem(), e));
                    scripts.add(s);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            });
        }
        return scripts;
    }

    private String getGameDirectory(RunnableCheat data) {
        return String.format("%s/%s", data.getDirectory(), data.getSystem());
    }

    private CheatFile readFile(RunnableCheat data) throws Exception {

        try (FileReader fr = new FileReader(String.format("%s/%s", getGameDirectory(data), data.getCht()))) {
            return CheatApplication.getGson().fromJson(fr, CheatFile.class);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public void exit() {
        closed = true;
        readMemory = null;
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
        if (trainerThread != null) {
            try {
                trainerThread.join();
                trainerThread = null;
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
        if (keyHandler != null) {
            GlobalScreen.removeNativeKeyListener(keyHandler);
            keyHandler = null;
        }
        ScanMap.reset();
        log.info("DESTROY INSTANCE");
        instance = null;
    }

    private void openProcess() {
        handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, (int) pid);
    }

    private int willSearch(WinNT.MEMORY_BASIC_INFORMATION mbi, List<RegionSizeRange> regionSize, long pos) {
        int res = 0;
        if ( ((mbi.protect.intValue() & PAGE_EXECUTE_READWRITE) != 0) || ((mbi.protect.intValue() & PAGE_READWRITE) != 0) ) {
            res = 1;
            if (regionSize.size() == 0 || regionSize.stream().anyMatch(size -> size.insideRange(mbi.regionSize.longValue()))) {
                res = 2;
                if (passes == 0 || regionSet.contains(pos)) {
                    res = 3;
                }
            }
        }
        return res;
    }

    public void performSearch() {

        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        long pos = 0;
        if (readMemory == null)
            readMemory = allocateMemory();
        scanMap.update(cheatList, scriptList);
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ( ((mbi.protect.intValue() & PAGE_EXECUTE_READWRITE) != 0) || ((mbi.protect.intValue() & PAGE_READWRITE) != 0) ) {
                if (regionSize.size() == 0 || regionSize.stream().anyMatch(size -> size.insideRange(mbi.regionSize.longValue()))) {
                    if (passes == 0 || regionSet.contains(pos)) { //this is a new region for some reason, let's ignore it.
                        seachMemory(mbi, pos);
                    }
                    else if (!regionSet.contains(pos)){
                        log.warn("New memory region found {}-{} {}, reallocating memory", FormatTools.valueToHex(pos), FormatTools.valueToHex(pos+mbi.regionSize.longValue()), mbi.regionSize.longValue());
                        readMemory = null;
                        break;
                        //readMemory = allocateMemory();
                        //seachMemory(mbi, pos);
                    }
                }
            }
            pos += mbi.regionSize.longValue();
        }
        passes++;
        searchComplete();
        scanMap.writeResults();
        if (readMemory == null) {
            passes = 0;
            regionSet.clear();
        }
    }

    private void seachMemory(WinNT.MEMORY_BASIC_INFORMATION mbi, long pos) {
        Kernel32.INSTANCE.ReadProcessMemory(handle, new Pointer(pos), readMemory, mbi.regionSize.intValue(), null);
        scanMap.search(pos, readMemory, mbi.regionSize.longValue());
        regionSet.add(pos);
        log.trace("Searching...");
    }

    private Memory allocateMemory() {
        long pos = 0;
        long maxMemory = Long.MIN_VALUE;
        if (!hasCheats()) {
            return new Memory(1);
        }
        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ( ((mbi.protect.intValue() & PAGE_EXECUTE_READWRITE) != 0) || ((mbi.protect.intValue() & PAGE_READWRITE) != 0) ) {
                maxMemory = Math.max(maxMemory, mbi.regionSize.longValue());
            }
            pos += mbi.regionSize.longValue();
        }
        log.info("Grabbing memory of size {}", maxMemory);
        return new Memory(maxMemory);
    }

    private void searchComplete() {
        int count = 0;
        if (cheatList != null) {
            for (io.Cheat cheat : cheatList) {
                if (!cheat.parentProcessingComplete())
                    continue;
                count += cheat.getResults().size();
                for (Code code : cheat.getCodes()) {
                    if (code.getOperations() != null && code.getOperations().size() > 0) {
                        code.getCurrentOperation().searchComplete(scanMap.getAllSearchResults(cheat));
                    }
                }
            }
        }
        if (scriptList != null) {
            for (Script script : scriptList) {
                try {
                    for (Cheat cheat : script.getAllCheats()) {
                        if (!cheat.parentProcessingComplete())
                            continue;
                        count += cheat.getResults().size();
                        for (Code code : cheat.getCodes()) {
                            if (code.getOperations() != null && code.getOperations().size() > 0) {
                                code.getCurrentOperation().searchComplete(scanMap.getAllSearchResults(cheat));
                            }
                        }
                    }
                    script.searchComplete();
                } catch (Exception ex) {
                    log.error("Script Error {}", ex.getMessage());
                }
            }
        }
        log.trace("Total results: {}", count);
    }


    public void writeCheats() {
        scanMap.write(cheatList, scriptList);
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
        if (!searchNeeded())
            return;
        if (scriptList.size() > 0 || cheatList.size() > 0) {
            performSearch();
        }
    }

    private boolean searchNeeded() {
        for (Cheat c : scanMap.getEveryCheat()) {
            if (c.isEnabled())
                return true;
        }
        return false;
    }

    public void start() {
        searchThread = new Thread(this::searchProcess);
        writeThread = new Thread(this::writeProcess);
        if (cheatFile.getTrainer() != null) {
            trainerThread = new Thread(this::trainProcess);
            trainerThread.start();
        }
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
        scanMap.write(cheatList, scriptList);
    }

    public void writeProcess() {
        while (!closed) {
            try {
                processWrites();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void searchProcess() {
        readMemory = allocateMemory();
        while (!closed) {
            try {
                if (hasCheats()) {
                    searchLoop();
                }
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.warn("Search thread interrupted!");
                break;
            } catch (Exception e) {
                log.error("Search Error {}", e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        messageQueue.add(new Message(new ProcessComplete(true)));
        log.info("Search and apply thread exited");
    }

    public void trainProcess() {
        while (!closed) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void toggleCheat(int id) throws Exception {
        io.Cheat cheat = findCheat(id);
        if (cheat == null) {
            log.error("Could not find cheat {}", id);
            return;
        }
        cheat.toggle();
    }

    public void triggerCheat(int id) throws Exception {
        io.Cheat cheat = findCheat(id);
        if (cheat == null) {
            log.error("Could not find cheat {}", id);
            return;
        }
        cheat.trigger(new Trigger.TriggerInfo(cheat.getTrigger().getBehavior(), 0, false));
    }


    public void resetCheat(int id) throws Exception {
        io.Cheat cheat = findCheat(id);
        if (cheat == null) {
            log.error("Could not find cheat {}", id);
            return;
        }
        cheat.queueReset();
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
    }

    public RunnableCheat getData() {
        return data;
    }

    public Game getGame() {
        return new Game(data.getSystem(), gameName, data.getCht());
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

    public static Process getInstance() {
        return instance;
    }

    public void triggerTrainer(HotKey key) {
        Trainer trainer = cheatFile.getTrainer();
        if (trainer == null)
            return;
        Optional<HotKey> op = trainer.getHotKeys().stream().filter(e -> e.equals(key)).findFirst();
        if (op.isPresent()) {
            HotKey hk = op.get();
            hk.trigger();
        }
    }

    private boolean noCheats() {
        boolean empty = true;
        if (cheatList != null && !cheatList.isEmpty())
            empty = false;
        if (scriptList != null) {
            if (scriptList.stream().anyMatch(s -> !s.getAllCheats().isEmpty()))
                empty = false;
        }
        return empty;
    }

    public boolean hasCheats() {
        return !noCheats();
    }
}
