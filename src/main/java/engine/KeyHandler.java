package engine;

import io.Cheat;
import io.Trigger;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class KeyHandler implements NativeKeyListener {
    static Logger log = LoggerFactory.getLogger(KeyHandler.class);
    private Map<Integer, Set<Cheat>> subscribeMap;

    public KeyHandler() {
        subscribeMap = new HashMap<>();
    }

    public void subscribe(int key, Cheat cheat) {
        if (!subscribeMap.containsKey(key)) {
            subscribeMap.put(key, new HashSet<>());
        }
        Set<Cheat> cheats = subscribeMap.get(key);
        cheats.add(cheat);
    }

    public List<Cheat> getOneTimeCheats(int key) {
        if (!subscribeMap.containsKey(key))
            return new ArrayList<>();
        return subscribeMap.get(key).stream().filter(cheat -> (cheat.getTrigger() != null && cheat.getTrigger().getBehavior() == Trigger.Behavior.ONETIME)).collect(Collectors.toList());
    }

    public List<Cheat> getToggleCheats(int key) {
        if (!subscribeMap.containsKey(key))
            return new ArrayList<>();
        return subscribeMap.get(key).stream().filter(cheat -> (cheat.getTrigger() != null && cheat.getTrigger().getBehavior() == Trigger.Behavior.TOGGLE)).collect(Collectors.toList());
    }

    public List<Cheat> getHoldCheats(int key) {
        if (!subscribeMap.containsKey(key))
            return new ArrayList<>();
        return subscribeMap.get(key).stream().filter(cheat -> (cheat.getTrigger() != null && cheat.getTrigger().getBehavior() == Trigger.Behavior.HOLD)).collect(Collectors.toList());
    }



    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        List<Cheat> holdCheats = getHoldCheats(nativeKeyEvent.getRawCode());
        holdCheats.forEach(cheat -> cheat.trigger(new Trigger.TriggerInfo(Trigger.Behavior.HOLD, nativeKeyEvent.getRawCode(), true)));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        List<Cheat> toggleCheats = getToggleCheats(nativeKeyEvent.getRawCode());
        List<Cheat> oneTimeCheats = getOneTimeCheats(nativeKeyEvent.getRawCode());
        List<Cheat> holdCheats = getHoldCheats(nativeKeyEvent.getRawCode());


        toggleCheats.forEach(cheat -> cheat.trigger(new Trigger.TriggerInfo(Trigger.Behavior.TOGGLE, nativeKeyEvent.getRawCode(), false)));
        oneTimeCheats.forEach(cheat -> cheat.trigger(new Trigger.TriggerInfo(Trigger.Behavior.ONETIME, nativeKeyEvent.getRawCode(), false)));
        holdCheats.forEach(cheat -> cheat.trigger(new Trigger.TriggerInfo(Trigger.Behavior.HOLD, nativeKeyEvent.getRawCode(), false)));


    }

    public void update(ScanMap scanMap) {
        scanMap.getEveryCheat()
                .stream()
                .filter(cheat -> cheat.getTrigger() != null && cheat.getTrigger().getType() == Trigger.Type.KEYPRESS)
                .forEach(cheat -> subscribe(cheat));

    }

    private void subscribe(Cheat cheat) {
        if (cheat.getTrigger() == null || cheat.getTrigger().getType() != Trigger.Type.KEYPRESS || cheat.getTrigger().getKey() == 0)
            return;
        int key = cheat.getTrigger().getKey();
        subscribe(key, cheat);
    }
}
