package cheat;

import java.util.Objects;

public class Cheat {
    protected int id;
    protected String name;
    protected boolean enabled;

    public Cheat(int id, String name) {
        this.id = id;
        this.name = name;
        this.enabled = true;
    }

    public Cheat(String name) {
        this(-1, name);
    }


    public int getId() {
        return id;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public void reset() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cheat cheat = (Cheat) o;
        return id == cheat.id &&
                enabled == cheat.enabled &&
                Objects.equals(name, cheat.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled);
    }
}
