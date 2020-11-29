package engine;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

public class Util {

    static public void createCheats(String name) {
        LocalResources res = new LocalResources();
        res.addDirectory("Cheat");
        res.addFile("Cheat", "cheat.cht");
        res.addFile("Cheat", "cheat2.cht");
        res.addFile("Cheat", "scripting.cht");
        res.addFile("Cheat", "testcode.cht");
        res.addFile("Cheat", "trainer.cht");
        res.addFile("Cheat/scripts", "script.js");
        res.addFile("Cheat/scripts", "testscript.js");
        res.addFile("Cheat/scripts", "inc.js");
        res.addFile("Cheat/trainers", "Trainer.exe");
        res.process(name);
    }

    static public void deleteCheats(String name) {
        try {
            Files.walk(Path.of(new File(name).getAbsolutePath()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {

        }
    }

    public static void setupKeyHandler() throws NativeHookException {
        if (Process.debugMode)
            return;
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);
        GlobalScreen.registerNativeHook();
    }
}
