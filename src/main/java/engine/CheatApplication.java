package engine;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.*;
import games.Game;
import games.RunnableCheat;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJson;
import message.*;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import response.Failure;
import response.GameList;
import response.Response;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class CheatApplication {
    static Logger log = LoggerFactory.getLogger(CheatApplication.class);
    final static String cheatDir = "./cheats";
    final private ArrayBlockingQueue<Message> messageQueue;
    final private Gson gson;
    final private CheatThread cheatThread;
    private Javalin app;

    public CheatApplication() {
        messageQueue = new ArrayBlockingQueue<>(10);
        gson = new GsonBuilder().create();
        JavalinJson.setToJsonMapper(gson::toJson);
        JavalinJson.setFromJsonMapper(gson::fromJson);
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
        app.post("/runGameCheat", ctx -> executeGameCheat(ctx));
        app.post("/toggleGameCheat", ctx -> toggleGameCheat(ctx));
        app.post("/resetGameCheat", ctx -> resetGameCheat(ctx));
        app.post("/exitCheat", ctx -> exitCheat(ctx));
        createDirectories();
        new Thread(cheatThread).start();
    }

    private void createDirectories() {
        LocalResources res = new LocalResources();
        res.addDirectory("DosBox");
        res.addFile("DosBox", "Privateer.cht");
        res.addFile("DosBox", "Wolfenstein 3D.cht");
        res.addFile("DosBox", "Strike Commander.cht");
        res.addFile("DosBox/Strike Commander", "Strike Commander - Ammo.js");
        res.addFile("DosBox/Strike Commander", "Strike Commander - Inv.js");
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
            CompletableFuture<Response> response = addMessageWithResponse(new Message(cht));
            ctx.json(response);
        } catch (Exception e) {
            log.error(e.getMessage());
            String fail = "{'status': 'failed'}";
            ctx.json(JsonParser.parseString(fail));
        }

    }

    private void populateGames(Context ctx, String system) {
        List<String> validFiles = new ArrayList();
        List<Game> games = new ArrayList<>();
        for (File file: Files.fileTraverser().breadthFirst(new File(cheatDir+"/"+system)))
        {
            if (file.isFile() && Files.getFileExtension(file.getAbsolutePath()).toUpperCase().equals("CHT")) {
                validFiles.add(file.getAbsolutePath());
            }
        }
        validFiles.stream().forEach(e -> {
            try {
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
        ctx.json(new GameList(games));
    }

    private void populateSystems(Context ctx) {
        File f = new File(cheatDir);
        String files[] = f.list();
        JSONArray res = new JSONArray();
        for (File file: Iterables.limit(Files.fileTraverser().breadthFirst(new File(cheatDir)), 2))
        {
            if (file.isDirectory()) {
                if (file.getAbsolutePath().equals(f.getAbsolutePath())) continue;
                res.add(file.getName());
            }
        }
        ctx.json(res);
    }
}
