package io;

import script.Value;

import java.util.Objects;

public class OVPair {
    private int offset;
    private Value value;

    public OVPair(int offset, Value value) {
        this.offset = offset;
        this.value = value;
    }

    public OVPair(int offset, String value) {
        this.offset = offset;
        this.value = Value.createValue(value);
    }


    public int getOffset() {
        return offset;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(String val) {
        value = Value.createValue(val);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OVPair ovPair = (OVPair) o;
        return offset == ovPair.offset &&
                Objects.equals(value, ovPair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, value);
    }

    @Override
    public String toString() {
        return String.format("Offset: %X [%s]", offset, value);
    }

    public String getAddress(long addr) {
        return String.format(String.format("Address: %X [%s]", addr+offset, value));
    }
}
