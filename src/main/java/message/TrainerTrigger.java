package message;

import io.HotKey;

public class TrainerTrigger implements MessageData {
    private HotKey item;

    public TrainerTrigger(HotKey item) {
        this.item = item;
    }

    public HotKey getHotKey() {
        return item;
    }
}
