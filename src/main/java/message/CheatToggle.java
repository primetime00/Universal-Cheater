package message;

public class CheatToggle implements MessageData {
    private int id;

    public CheatToggle(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
