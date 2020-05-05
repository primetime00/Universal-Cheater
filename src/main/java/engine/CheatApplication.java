package engine;

import cheat.AOB;
import com.google.common.collect.Iterables;
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
import script.Value;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
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
    private ApplicationState currentState;

    public CheatApplication() {
        messageQueue = new ArrayBlockingQueue<>(10);
        JavalinJson.setToJsonMapper(getGson()::toJson);
        JavalinJson.setFromJsonMapper(getGson()::fromJson);
        currentState = new ApplicationState();
        cheatThread = new CheatThread(messageQueue);
    }

    public void start() {
        app = Javalin.create(config -> config
                .enableWebjars()
                .addStaticFiles("/web", Location.CLASSPATH)
        ).start(7000);
        app.get("/", ctx -> ctx.render("web/index.html"));
        app.get("/getSystems", ctx -> populateSystems(ctx));
        app.get("/getGameCheats/:system", ctx -> populateGames(ctx, ctx.pathParam("system")));
        app.get("/getCheatStatus", ctx -> getCheatStatus(ctx));
        app.get("/getAppStatus", ctx -> getAppStatus(ctx));
        app.error(404, ctx -> {log.error("ERROR!"); ctx.render("web/index.html");});
        app.post("/setAppState", ctx -> setAppState(ctx));
        app.post("/runGameCheat", ctx -> executeGameCheat(ctx));
        app.post("/toggleGameCheat", ctx -> toggleGameCheat(ctx));
        app.post("/triggerGameCheat", ctx -> triggerGameCheat(ctx));
        app.post("/resetGameCheat", ctx -> resetGameCheat(ctx));
        app.post("/exitCheat", ctx -> exitCheat(ctx));
        createDirectories();
        try {
            installKeyHook();
        } catch (NativeHookException e) {
            log.error("Cannot register key hooks: {}", e.getMessage());
            return;
        }
        new Thread(cheatThread).start();
    }

    private void setAppState(Context ctx) {
        int st = Integer.parseInt(ctx.body());
        currentState.setState(st);
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
        LocalResources res = new LocalResources();
        res.addDirectory("DosBox");
        res.addFile("DosBox", "Privateer.cht");
        res.addFile("DosBox", "Wolfenstein 3D.cht");
        res.addFile("DosBox", "Master Of Orion 2.cht");
        res.addFile("DosBox", "Strike Commander.cht");
        res.addFile("DosBox/scripts/Strike Commander", "Strike Commander - Ammo.js");
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
        for (File file: Iterables.limit(Files.fileTraverser().breadthFirst(new File(cheatDir)), 2))
        {
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
