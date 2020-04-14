package cheat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StaticCheat extends Cheat {
    private transient MasterCode master;
    private List<Code> codes;

    public StaticCheat(MasterCode master, int id, String name) {
        super(id, name);
        this.master = master;
        this.codes = new ArrayList<>();
    }

    public MasterCode getMaster() {
        return master;
    }

    public List<Code> getCodes() {
        return codes;
    }

    public void addCode(Code code) {
        codes.add(code);
    }

    @Override
    public void reset() {
        master.reset();
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return Objects.hash(hash, codes);
    }
}
