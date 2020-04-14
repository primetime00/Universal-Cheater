package message;

public class CheatReset implements MessageData {
    private int id;

    public CheatReset(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
