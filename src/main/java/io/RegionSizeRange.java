package io;

public class RegionSizeRange {
    long low;
    long high;

    public RegionSizeRange(long low, long high) {
        this.low = low;
        this.high = high;
    }

    public long getLow() {
        return low;
    }

    public long getHigh() {
        return high;
    }

    public boolean insideRange(long size) {
        return size >= low && size <= high;
    }
}
