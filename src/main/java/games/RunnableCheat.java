package games;

import message.MessageData;

import java.util.Objects;

public class RunnableCheat implements MessageData {
    private String directory;
    private String system;
    private String cht;

    public RunnableCheat(String directory, String system, String cht) {
        this.directory = directory;
        this.system = system;
        this.cht = cht;
    }

    public String getDirectory() {
        return directory;
    }

    public String getSystem() {
        return system;
    }

    public String getCht() {
        return cht;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public void setCht(String cht) {
        this.cht = cht;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunnableCheat that = (RunnableCheat) o;
        return Objects.equals(directory, that.directory) &&
                Objects.equals(system, that.system) &&
                Objects.equals(cht, that.cht);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory, system, cht);
    }
}
