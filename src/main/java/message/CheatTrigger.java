package message;

public class CheatTrigger implements MessageData {
    private int id;

    public CheatTrigger(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
