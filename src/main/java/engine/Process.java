package engine;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import games.Game;
import games.RunnableCheat;
import io.Cheat;
import io.CheatFile;
import io.Code;
import io.Trigger;
import message.Message;
import message.ProcessComplete;
import org.jnativehook.GlobalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.Script;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class Process {
    static Logger log = LoggerFactory.getLogger(Process.class);
    static private Process instance = null;
    static public boolean debugMode = false;
    private int pid;
    private static WinNT.HANDLE handle = null;
    private WinNT.HWND selectedHWND;
    CheatFile cheatFile;
    List<io.Cheat> cheatList;
    List<Script> scriptList;
    private List<Long> regionSize;
    private boolean closed;
    private Thread searchThread;
    private Thread writeThread;
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
        this.pid = getProcessID(cheatFile.getWindow());
        openProcess();
        this.gameName = cheatFile.getGame();
        this.cheatList = cheatFile.getCheats();
        this.scriptList = createScripts(data);
        this.regionSize = cheatFile.getRegionSize();
        keyHandler = new KeyHandler();
        if (!debugMode) {
            GlobalScreen.addNativeKeyListener(keyHandler);
            this.scanMap.addUpdateHandler(() -> keyHandler.update(scanMap));
        }
        this.scanMap.update(this.cheatList, this.scriptList);
        if (cheatList != null)
            this.cheatList.stream().filter(e->e.getScriptHandler() != null).forEach(c -> c.getScriptHandler().initialize(c));

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

    private CheatFile readFile(RunnableCheat data) throws Exception {

        try (FileReader fr = new FileReader(String.format("%s/%s/%s", data.getDirectory(), data.getSystem(), data.getCht()))) {
            return CheatApplication.getGson().fromJson(fr, CheatFile.class);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private int getProcessID(CheatFile.Window window) throws Exception {
        selectedHWND = null;
        int count = 0;
        while (count < 5) {
            User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
                String wText = Native.toString(windowText);
                if (wText.contains(window.getWindowTitle())) {
                    selectedHWND = hwnd;
                    return false;
                }
                return true;
            }, null);
            if (selectedHWND != null)
                break;
            count++;
            Thread.sleep(500);
        }

        if (selectedHWND == null) {
            throw new Exception("Could not find process!");
        }
        IntByReference ref = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(selectedHWND, ref);
        return ref.getValue();
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
        if (keyHandler != null) {
            GlobalScreen.removeNativeKeyListener(keyHandler);
            keyHandler = null;
        }
        ScanMap.reset();
        log.info("DESTROY INSTANCE");
        instance = null;
    }

    private void openProcess() {
        handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, pid);
    }

    public void performSearch() {
        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        long pos = 0;
        if (readMemory == null)
            readMemory = allocateMemory();
        scanMap.update(cheatList, scriptList);
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ((mbi.protect.intValue() & PAGE_READWRITE) != 0) {
                if (regionSize.size() == 0 || regionSize.stream().anyMatch(size -> size == mbi.regionSize.longValue())) {
                    if (passes == 0 || regionSet.contains(pos)) { //this is a new region for some reason, let's ignore it.
                        Kernel32.INSTANCE.ReadProcessMemory(handle, new Pointer(pos), readMemory, mbi.regionSize.intValue(), null);
                        scanMap.search(pos, readMemory, mbi.regionSize.longValue());
                        regionSet.add(pos);
                        log.trace("Searching...");
                    }
                }
            }
            pos += mbi.regionSize.longValue();
        }
        passes++;
        searchComplete();
        scanMap.writeResults();
    }

    private Memory allocateMemory() {
        long pos = 0;
        long maxMemory = Long.MIN_VALUE;
        WinNT.MEMORY_BASIC_INFORMATION mbi = new WinNT.MEMORY_BASIC_INFORMATION();
        while (Kernel32.INSTANCE.VirtualQueryEx(handle, new Pointer(pos), mbi, new BaseTSD.SIZE_T(mbi.size())).intValue() != 0) {
            if ((mbi.protect.intValue() & PAGE_READWRITE) != 0) {
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
                searchLoop();
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
}
