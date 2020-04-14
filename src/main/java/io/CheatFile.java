package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CheatFile {
    static Logger log = LoggerFactory.getLogger(CheatFile.class);
    String game;
    Window window;
    List<Long> regionSize;
    List<MasterCode> masterCodes;
    List<String> scripts;

    public String getGame() {
        return game;
    }

    public Window getWindow() {
        return window;
    }

    public List<MasterCode> getMasterCodes() {
        return masterCodes;
    }

    public List<Long> getRegionSize() {
        return regionSize;
    }

    public List<String> getScripts() {
        return scripts;
    }

    static public class MasterCode {
        private String master;
        private Search search;
        private Change change;
        private boolean researchAfterFound = true;

        private List<Cheat> cheats;

        public String getMaster() {
            return master;
        }

        public List<Cheat> getCheats() {
            return cheats;
        }

        public boolean isResearchAfterFound() {
            return researchAfterFound;
        }

        public Search getSearch() {
            return search;
        }

        public Change getChange() {
            return change;
        }
    }

    static public class Cheat {
        private String name;
        private List<Code> codes;

        public String getName() {
            return name;
        }

        public List<Code> getCodes() {
            return codes;
        }

        public int getId() {
            return hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cheat cheat = (Cheat) o;
            return Objects.equals(name, cheat.name) &&
                    Objects.equals(codes, cheat.codes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, codes);
        }
    }

    static public class Code {
        private int offset;
        private String value;

        public int getOffset() {
            return offset;
        }

        public String getValue() {
            return value;
        }

    }

    static public class Search {
        int offset;
        String low;
        String high;
        boolean found = false;

        public int getOffset() {
            return offset;
        }

        public String getLow() {
            return low;
        }

        public String getHigh() {
            return high;
        }

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean found) {
            this.found = found;
        }
    }

    static public class Change {
        private int offset;
        private long delay = 500;
        private Map<Long, ChangeRecord> recordMap = new HashMap<>();

        public boolean has(long index) {
            return recordMap.containsKey(index);
        }

        public void reset() {
            recordMap.clear();
        }

        static public class ChangeRecord {
            private byte recordedValue;
            private boolean hasRecord = false;
            private long elapsed = 0;
            private final long address;
            private final long delay;

            public ChangeRecord(long delay, long address) {
                this.delay = delay;
                this.address = address;
            }

            public long getElapsed() {
                return elapsed;
            }

            public void mark() {
                log.trace("Marking change on address {}", address);
                this.elapsed = System.currentTimeMillis();
            }

            public boolean started() {
                return this.elapsed > 0;
            }

            public void recordValue(byte value) {
                log.trace("Recording value {}", value);
                recordedValue = value;
                hasRecord = true;
            }

            public boolean isHasRecord() {
                return hasRecord;
            }



            public boolean expired() {
                return System.currentTimeMillis() - this.elapsed >= this.delay;
            }

            public boolean valueChanged(byte aByte) {
                return aByte != recordedValue;
            }

        }

        public int getOffset() {
            return offset;
        }

        public long getDelay() {
            return delay;
        }

        public void add(long address) {
            recordMap.put(address, new ChangeRecord(delay, address));
        }

        public ChangeRecord get(long address) {
            return recordMap.get(address);
        }


    }


    static public class Window {
        private String windowClass;
        private String windowTitle;
        private boolean partialMatch;

        public String getWindowClass() {
            return windowClass;
        }

        public String getWindowTitle() {
            return windowTitle;
        }

        public boolean isPartialMatch() {
            return partialMatch;
        }

    }

}
