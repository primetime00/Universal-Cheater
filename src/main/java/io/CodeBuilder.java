package io;

import java.util.ArrayList;
import java.util.List;

public class CodeBuilder {

    private List<OVPair> offsets;
    protected List<OperationProcessor> operations;

    public CodeBuilder() {
    }

    public CodeBuilder(int offset, String value) {
        offsets = new ArrayList<>();
        offsets.add(new OVPair(offset, value));
    }

    public CodeBuilder addOffset(int offset, String value) {
        if (offsets == null)
            offsets = new ArrayList<>();
        offsets.add(new OVPair(offset, value));
        return this;
    }

    public CodeBuilder addOperation(OperationProcessor op) {
        if (operations == null)
            operations = new ArrayList<>();
        operations.add(op);
        return this;
    }

    public Code build() {
        return new Code(offsets, operations);
    }
}
