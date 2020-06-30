package io;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.sun.jna.Memory;
import engine.ScanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import util.FormatTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Instance implements OperationProcessor{
    static Logger log = LoggerFactory.getLogger(Instance.class);
    private Cheat instanceCheat;
    private int identity;
    private int scanCount = 0;
    private boolean found = false;
    private boolean complete = false;

    public Instance(int identity) {
        this.identity = identity;
    }

    public Instance() {
    }

    @Override
    public void process(Collection<ArraySearchResult> results, long pos, Memory mem) {
    }

    private void filterResults(Collection<ArraySearchResult> originalResults, List<ArraySearchResult> otherResult) {
        for (ArraySearchResult res : otherResult) {
            for (ArraySearchResult mRes : originalResults) {
                mRes.setValid(false);
                if (mRes.getAddress() == res.getAddress()) {
                    mRes.setValid(true);
                    complete = true;
                }
            }
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void searchComplete(Collection<ArraySearchResult> results) {
        List<ArraySearchResult> validResults = ScanMap.get().getAllSearchResults().stream().filter(ArraySearchResult::isValid).collect(Collectors.toList());
        if (!found) {
            if (scanCount%10 != 0)
                return;
            Optional<Cheat> foundCheat = ScanMap.get().getEveryCheat().stream().filter(cheat -> cheat.getIdentity() > 0 && cheat.getIdentity() == identity).findFirst();
            if (foundCheat.isPresent()) {
                instanceCheat = foundCheat.get();
                found = true;
            }
            scanCount++;
        }
        if (found && !complete && instanceCheat != null) {
            if (instanceCheat.hasOperations()) {
                if (instanceCheat.getCodes().stream().allMatch(Code::operationsComplete)) {
                    List<ArraySearchResult> instanceResults = ScanMap.get().getAllSearchResults(instanceCheat).stream().filter(ArraySearchResult::isValid).collect(Collectors.toList());
                    filterResults(results, instanceResults);
                }
            }
            else {
                filterResults(results, new ArrayList(instanceCheat.getResults()));
            }
        }
        if (complete) {
            log.trace("Instance Found valid address at {}", FormatTools.valueToHex(validResults.get(0).getAddress()));
        }
    }

    @Override
    public void readJson(JsonObject data, JsonDeserializationContext ctx) {
        this.identity = data.get("identity").getAsInt();
    }

    public int getIdentity() {
        return identity;
    }

    @Override
    public void reset() {
        scanCount = 0;
        instanceCheat = null;
        found = false;
        complete = false;
    }
}
