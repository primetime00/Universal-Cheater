package message;

public class ProcessComplete implements MessageData {
    boolean terminated;

    public ProcessComplete(boolean terminated) {
        this.terminated = terminated;
    }

    public boolean isTerminated() {
        return terminated;
    }
}
