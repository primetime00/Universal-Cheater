package trainer;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenCapture {

    private WinDef.HWND callBackHwnd;
    private Robot robot;

    public WinDef.HWND getWinHwnd(final String startOfWindowName) {
        User32.INSTANCE.EnumWindows((hWnd, pointer) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            String wText = Native.toString(windowText).trim();

            if (!wText.isEmpty() && wText.startsWith(startOfWindowName)) {
                callBackHwnd = hWnd;
                return false;
            }
            return true;

        }, null);
        return callBackHwnd;
    }

    private void save(WinDef.RECT rec) {
        try {
            robot = new Robot();
            BufferedImage img = robot.createScreenCapture(rec.toRectangle());
            File f = new File("cap.jpg");
            ImageIO.write(img, "jpg", f);
            System.out.println(String.format("Write image %s", f.getAbsolutePath()));
        } catch (Exception e) {
            System.err.println (String.format("Could not write image: %s", e.getMessage()));
        }
    }

    public void captureWindow(final String name) throws AWTException {
        WinDef.HWND hwnd = getWinHwnd(name);
        WinDef.RECT rec = new WinDef.RECT();
        WinDef.HWND active = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.SetForegroundWindow(hwnd);
        User32.INSTANCE.GetWindowRect(hwnd, rec);
        save(rec);
        User32.INSTANCE.SetForegroundWindow(active);
        User32.INSTANCE.SetFocus(active);
        robot = new Robot();
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_ALT);
    }

    public static void main(String[] args) throws AWTException {
        ScreenCapture cap = new ScreenCapture();
        cap.captureWindow("Calculator");
    }

}
