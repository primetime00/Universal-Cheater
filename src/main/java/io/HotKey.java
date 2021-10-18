package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

public class HotKey {
    static Logger log = LoggerFactory.getLogger(HotKey.class);
    private String name;
    private List<String> keys;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotKey hotKey = (HotKey) o;
        return Objects.equals(name, hotKey.name) &&
                Objects.equals(keys, hotKey.keys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keys);
    }

    public void trigger() {

        try {
            Robot robot = new Robot();
            HotKeyEvent evt = createKeyEvent();
            doKey(robot, evt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doKey(Robot robot, HotKeyEvent evt) throws InterruptedException {
        if (evt.getMod() != 0) {
            robot.keyPress(evt.getMod());
            Thread.sleep(25);
        }
        if (evt.getKeycode() != 0) {
            robot.keyPress(evt.getKeycode());
        }
        Thread.sleep(100);
        if (evt.getKeycode() != 0) {
            robot.keyRelease(evt.getKeycode());
            Thread.sleep(25);
        }
        if (evt.getMod() != 0) {
            robot.keyRelease(evt.getMod());
        }

    }

    private HotKeyEvent createKeyEvent()  {
        int mod = 0x00;
        int code = 0x00;
        for (String key : keys) {
            switch (key.toLowerCase()) {
                case "lctrl":
                case "ctrl":
                    mod = KeyEvent.VK_CONTROL;
                    continue;
                case "rctrl":
                    mod = KeyEvent.VK_CONTROL;
                    continue;
                case "lalt":
                case "alt":
                    mod = KeyEvent.VK_ALT;
                    continue;
                case "ralt":
                    mod = KeyEvent.VK_ALT;
                    continue;
                case "lshift":
                case "shift":
                    mod = KeyEvent.VK_SHIFT;
                    continue;
                case "rshift":
                    mod = KeyEvent.VK_SHIFT;
                    continue;
                default:
                    break;
            }
            switch (key.toLowerCase()) {
                case "a": code = KeyEvent.VK_A; break;
                case "b": code = KeyEvent.VK_B; break;
                case "c": code = KeyEvent.VK_C; break;
                case "d": code = KeyEvent.VK_D; break;
                case "e": code = KeyEvent.VK_E; break;
                case "f": code = KeyEvent.VK_F; break;
                case "g": code = KeyEvent.VK_G; break;
                case "h": code = KeyEvent.VK_H; break;
                case "i": code = KeyEvent.VK_I; break;
                case "j": code = KeyEvent.VK_J; break;
                case "k": code = KeyEvent.VK_K; break;
                case "l": code = KeyEvent.VK_L; break;
                case "m": code = KeyEvent.VK_M; break;
                case "n": code = KeyEvent.VK_N; break;
                case "o": code = KeyEvent.VK_O; break;
                case "p": code = KeyEvent.VK_P; break;
                case "q": code = KeyEvent.VK_Q; break;
                case "r": code = KeyEvent.VK_R; break;
                case "s": code = KeyEvent.VK_S; break;
                case "t": code = KeyEvent.VK_T; break;
                case "u": code = KeyEvent.VK_U; break;
                case "v": code = KeyEvent.VK_V; break;
                case "w": code = KeyEvent.VK_W; break;
                case "x": code = KeyEvent.VK_X; break;
                case "y": code = KeyEvent.VK_Y; break;
                case "z": code = KeyEvent.VK_Z; break;
                case "`": code = KeyEvent.VK_BACK_QUOTE; break;
                case "0": code = KeyEvent.VK_0; break;
                case "1": code = KeyEvent.VK_1; break;
                case "2": code = KeyEvent.VK_2; break;
                case "3": code = KeyEvent.VK_3; break;
                case "4": code = KeyEvent.VK_4; break;
                case "5": code = KeyEvent.VK_5; break;
                case "6": code = KeyEvent.VK_6; break;
                case "7": code = KeyEvent.VK_7; break;
                case "8": code = KeyEvent.VK_8; break;
                case "9": code = KeyEvent.VK_9; break;
                case "f1": code = KeyEvent.VK_F1; break;
                case "f2": code = KeyEvent.VK_F2; break;
                case "f3": code = KeyEvent.VK_F3; break;
                case "f4": code = KeyEvent.VK_F4; break;
                case "f5": code = KeyEvent.VK_F5; break;
                case "f6": code = KeyEvent.VK_F6; break;
                case "f7": code = KeyEvent.VK_F7; break;
                case "f8": code = KeyEvent.VK_F8; break;
                case "f9": code = KeyEvent.VK_F9; break;
                case "f10": code = KeyEvent.VK_F10; break;
                case "f11": code = KeyEvent.VK_F11; break;
                case "f12": code = KeyEvent.VK_F12; break;
                case "num0": code = KeyEvent.VK_NUMPAD0; break;
                case "num1": code = KeyEvent.VK_NUMPAD1; break;
                case "num2": code = KeyEvent.VK_NUMPAD2; break;
                case "num3": code = KeyEvent.VK_NUMPAD3; break;
                case "num4": code = KeyEvent.VK_NUMPAD4; break;
                case "num5": code = KeyEvent.VK_NUMPAD5; break;
                case "num6": code = KeyEvent.VK_NUMPAD6; break;
                case "num7": code = KeyEvent.VK_NUMPAD7; break;
                case "num8": code = KeyEvent.VK_NUMPAD8; break;
                case "num9": code = KeyEvent.VK_NUMPAD9; break;
                case "num.": code = KeyEvent.VK_DECIMAL; break;
                case "num+": code = KeyEvent.VK_ADD; break;
                case "num-": code = KeyEvent.VK_SUBTRACT; break;
                case "-": code = KeyEvent.VK_MINUS; break;
                case "=": code = KeyEvent.VK_EQUALS; break;
                case "enter": code = KeyEvent.VK_ENTER; break;
                case "[": code = KeyEvent.VK_OPEN_BRACKET; break;
                case "]": code = KeyEvent.VK_CLOSE_BRACKET; break;
                case "\\": code =KeyEvent.VK_BACK_SLASH; break;
                case ";": code = KeyEvent.VK_SEMICOLON; break;
                case ",": code = KeyEvent.VK_COMMA; break;
                case ".": code = KeyEvent.VK_PERIOD; break;
                case "/": code = KeyEvent.VK_SLASH; break;
                case "space": code = KeyEvent.VK_SPACE; break;
                case "pgup": code = KeyEvent.VK_PAGE_UP; break;
                case "pgdn": code = KeyEvent.VK_PAGE_DOWN; break;
                default:
                    throw new IllegalArgumentException("Cannot type " + key.toLowerCase());
            }
        }
        return new HotKeyEvent(code, mod);
    }

    public static class HotKeyEvent {
        int keycode;
        int mod;

        public HotKeyEvent(int keycode, int mod) {
            this.keycode = keycode;
            this.mod = mod;
        }

        public int getKeycode() {
            return keycode;
        }

        public int getMod() {
            return mod;
        }
    }
}
