package games;

import message.MessageData;

import java.util.Objects;

public class RunnableCheat implements MessageData {
    private String system;
    private String cht;

    public RunnableCheat(String system, String cht) {
        this.system = system;
        this.cht = cht;
    }

    public String getSystem() {
        return system;
    }

    public String getCht() {
        return cht;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunnableCheat that = (RunnableCheat) o;
        return Objects.equals(system, that.system) &&
                Objects.equals(cht, that.cht);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, cht);
    }
}
