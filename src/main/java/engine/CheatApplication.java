package engine;

import cheat.AOB;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.*;
import games.Game;
import games.RunnableCheat;
import io.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJson;
import message.*;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import response.Failure;
import response.GameList;
import response.Response;
import response.TrainerOptions;
import script.Value;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CheatApplication {
    static Logger log = LoggerFactory.getLogger(CheatApplication.class);
    final static String cheatDir = "./cheats";
    final private ArrayBlockingQueue<Message> messageQueue;
    private static Gson gson;
    final private CheatThread cheatThread;
    private Javalin app;
    private Thread tThread;
    static private final int port = 7000;

    public CheatApplication() {
        messageQueue = new ArrayBlockingQueue<>(10);
        JavalinJson.setToJsonMapper(getGson()::toJson);
        JavalinJson.setFromJsonMapper(getGson()::fromJson);
        cheatThread = new CheatThread(messageQueue);
    }

    public void start() {
        app = Javalin.create(config -> config
                .enableWebjars()
                .addStaticFiles("/web", Location.CLASSPATH)
        ).start(port);
        app.get("/", ctx -> ctx.render("web/index.html"));
        app.get("/getSystems", ctx -> populateSystems(ctx));
        app.get("/getGameCheats/:system", ctx -> populateGames(ctx, ctx.pathParam("system")));
        app.get("/getCheatStatus", ctx -> getCheatStatus(ctx));
        app.get("/getAppStatus", ctx -> getAppStatus(ctx));
        app.error(404, ctx -> {log.error("ERROR!"); ctx.render("web/index.html");});
        app.post("/runGameCheat", ctx -> executeGameCheat(ctx));
        app.get("/getGameTrainer", ctx -> ctx.json(getGameTrainer()));
        app.post("/toggleGameCheat", ctx -> toggleGameCheat(ctx));
        app.post("/triggerGameCheat", ctx -> triggerGameCheat(ctx));
        app.post("/triggerTrainer", ctx -> triggerTrainer(ctx));
        app.post("/resetGameCheat", ctx -> resetGameCheat(ctx));
        app.post("/exitCheat", ctx -> exitCheat(ctx));
        createDirectories();
        installTray();
        try {
            installKeyHook();
        } catch (NativeHookException e) {
            log.error("Cannot register key hooks: {}", e.getMessage());
            return;
        }
        tThread = new Thread(cheatThread);
        tThread.start();
    }

    private void installTray() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            URL url = this.getClass().getResource("/image/ghost.png");
            Image image = Toolkit.getDefaultToolkit().getImage(url);
            ActionListener listener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URI(String.format("http://localhost:%d", port)));
                        } catch (Exception e) {
                            log.error("Could not open browser: {}", e.getMessage());
                        }
                    }
                }
            };
            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Exit");
            final TrayIcon trayIcon = new TrayIcon(image, "Universal Cheater", popup);
            defaultItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ExitMessage msg = new ExitMessage();
                    addMessage(new Message(msg));
                    try {
                        tThread.join();
                    } catch (InterruptedException e) {
                        log.error("Cannot exit Universal Cheater");
                    }
                    app.stop();
                    tray.remove(trayIcon);
                    System.exit(0);

                }
            });
            popup.add(defaultItem);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(listener);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void getAppStatus(Context ctx) {
        CompletableFuture<Response> response = addMessageWithResponse(new Message(new CheatStatus()));
        ctx.json(response);
    }

    private void installKeyHook() throws NativeHookException {
        if (Process.debugMode)
            return;
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);
        GlobalScreen.registerNativeHook();
    }

    private void createDirectories() {
        Collection<ResourceList.ResourceAndDirectory> rl = ResourceList.getResourcesAndDirectories("cheat_codes");
        Collection<String> dirs = ResourceList.getDistinctDirectories(rl);
        LocalResources res = new LocalResources();
        dirs.forEach(res::addDirectory);
        rl.forEach(f -> res.addFile(f.getDirectory(), f.getFile()));
        res.process();
    }

    private void createCheatFile(File f) {
        URL url = getClass().getResource("/cheat_codes/DosBox/Wolfenstein 3D.cht");
        try (FileOutputStream os = new FileOutputStream(f)) {
            ByteStreams.copy(url.openStream(), os);
        } catch (Exception e) {
            log.error("Could not copy cheat file: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleGameCheat(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.body()); //should just be a primitive
            CompletableFuture<Response> response = addMessageWithResponse(new Message(new CheatToggle(id)));
            ctx.json(response);
        } catch (Exception e) {
            ctx.json(new Failure("Could not parse message"));
        }
    }

    private void triggerGameCheat(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.body()); //should just be a primitive
            CompletableFuture<Response> response = addMessageWithResponse(new Message(new CheatTrigger(id)));
            ctx.json(response);
        } catch (Exception e) {
            ctx.json(new Failure("Could not parse message"));
        }
    }

    private void triggerTrainer(Context ctx) {
        try {
            HotKey key = ctx.bodyAsClass(HotKey.class);
            CompletableFuture<Response> response = addMessageWithResponse(new Message(new TrainerTrigger(key)));
            ctx.json(response);
        } catch (Exception e) {
            ctx.json(new Failure("Could not parse message"));
        }
    }



    private void resetGameCheat(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.body()); //should just be a primitive
            CompletableFuture<Response> response = addMessageWithResponse(new Message(new CheatReset(id)));
            ctx.json(response);
        } catch (Exception e) {
            ctx.json(new Failure("Could not parse message"));
        }
    }


    private void getCheatStatus(Context ctx) {
        CompletableFuture<Response> response = addMessageWithResponse(new Message(new CheatStatus()));
        ctx.json(response);
    }

    private CompletableFuture<Response> addMessageWithResponse(Message msg) {
        CompletableFuture<Response> response = new CompletableFuture<>();
        msg.setResponse(response);
        addMessage(msg);
        return response;
    }

    private void addMessage(Message message) {
        messageQueue.add(message);
    }

    private void exitCheat(Context ctx) {
        ctx.json(addMessageWithResponse(new Message(new ExitCheat())));
    }

    public static void main(String[] args) {
        Process.debugMode = true;
        CheatApplication cheatApplication = new CheatApplication();
        cheatApplication.start();
    }

    private void executeGameCheat(Context ctx) {
        try {
            RunnableCheat cht = ctx.bodyAsClass(RunnableCheat.class);
            if (cht.getDirectory() == null)
                cht.setDirectory(CheatApplication.cheatDir);
            CompletableFuture<Response> response = addMessageWithResponse(new Message(cht));
            ctx.json(response);
        } catch (Exception e) {
            log.error(e.getMessage());
            String fail = "{'status': 'failed'}";
            ctx.json(JsonParser.parseString(fail));
        }

    }

    private Response getGameTrainer() {
        if (Process.getInstance() == null) {
            log.error("No process is running...");
            return new Failure("No process running...");
        }
        Trainer trainer = Process.getInstance().getCheatFile().getTrainer();
        if (trainer == null) {
            return new Failure("No trainer options...");
        }
        return new TrainerOptions(trainer.getHotKeys());
    }


    private void populateGames(Context ctx, String system) {
        ctx.json(populateGameList(getGson(), system, cheatDir));
    }

    public static GameList populateGameList(Gson gson, String system, String cheatDir) {
        List<String> validFiles = new ArrayList();
        List<Game> games = new ArrayList<>();
        for (File file: Files.fileTraverser().breadthFirst(new File(cheatDir +"/"+system)))
        {
            if (file.isFile() && Files.getFileExtension(file.getAbsolutePath()).toUpperCase().equals("CHT")) {
                validFiles.add(file.getAbsolutePath());
            }
        }
        validFiles.stream().forEach(e -> {
            try {
                CheatFile cheatFile = getGson().fromJson(new FileReader(e), CheatFile.class);
                JsonElement root = JsonParser.parseReader(new FileReader(e));
                if (!root.isJsonObject())
                    return;
                JsonObject obj = (JsonObject) root;
                if (!obj.has("game"))
                    return;
                String cht = Files.getNameWithoutExtension(e);
                games.add(new Game(system, obj.get("game").getAsString(), cht+".cht"));
            } catch (FileNotFoundException ex) {
                System.out.println(String.format("Could not find file %s", e));
            }
        });
        return new GameList(games);
    }

    private void populateSystems(Context ctx) {
        JSONArray res = new JSONArray();
        res.addAll(populateSystems(cheatDir));
        ctx.json(res);
    }

    public static List<String> populateSystems(String cheatDir) {
        File f = new File(cheatDir);
        List<String> res = new ArrayList<>();
        for (File file: new File(cheatDir).listFiles()) {
            if (file.isDirectory()) {
                if (file.getAbsolutePath().equals(f.getAbsolutePath())) continue;
                res.add(file.getName());
            }

        }
        return res;
    }

    public static Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Value.class, new Value.ValueDeserializer())
                    .registerTypeAdapter(AOB.class, new AOB.AOBDeserializer())
                    .registerTypeAdapter(Cheat.class, new Cheat.CheatDeserializer())
                    .registerTypeAdapter(Code.class, new Code.CodeDeserializer())
                    .registerTypeAdapter(Trigger.class, new Trigger.TriggerDeserializer())
                    .registerTypeAdapter(OperationProcessor.class, new OperationProcessor.AOBDeserializer())
                    .create();
        }
        return gson;
    }
}
