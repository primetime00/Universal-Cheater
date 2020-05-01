package engine;

import com.google.common.io.ByteStreams;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnitCom {
    private Socket client;

    public UnitCom() {
    }

    public int runUnitApp() throws IOException {
        URL url = getClass().getResource("/app/UnitTestApp.exe");
        File dir = new File("testApp");
        dir.mkdirs();
        File appFile = new File(dir, "UnitTestApp.exe");
        if (!appFile.exists()) {
            try (FileOutputStream os = new FileOutputStream(appFile)) {
                ByteStreams.copy(url.openStream(), os);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath());
        //java.lang.Process p = pb.start();

        Desktop d = Desktop.getDesktop();
        d.open(appFile);

        char title[] = new char[1024];
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow("ConsoleWindowClass", null);
        User32.INSTANCE.GetWindowText(hwnd, title, 1024);
        IntByReference ref = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, ref);
        return ref.getValue();


    }

    public void connect(int port) throws IOException {
        client = new Socket("localhost", port);
    }

    public void quit() throws IOException {
        byte [] data = {'@', 'q', 'u', 'i', 't'};
        client.getOutputStream().write(data);
        verify();
    }

    public void killStructure(byte id) throws IOException {
        byte [] data = {'@', 'k', 'i', 'l', 'l', '$', id};
        client.getOutputStream().write(data);
        verify();
    }

    public void changeTestValue(byte id, byte val) throws IOException {
        byte [] data = {'@', 'c', 'h', 'a', 'n', 'g', 'e', '$', id, val};
        client.getOutputStream().write(data);
        verify();
    }

    public void aobChange(byte id) throws IOException {
        byte [] data = {'@', 'a', 'o', 'b', '$', id};
        client.getOutputStream().write(data);
        verify();
    }

    public void verify() throws IOException {
        byte[] incoming = new byte[512];
        client.getInputStream().read(incoming);
        ByteBuffer bb = ByteBuffer.wrap(incoming, 0, 4);
        assert bb.order(ByteOrder.LITTLE_ENDIAN).getInt() == 0x6B636140;
    }


}
