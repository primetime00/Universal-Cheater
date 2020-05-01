package io;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.sun.jna.Memory;
import engine.ScanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;
import util.FormatTools;

import java.util.List;
import java.util.Optional;

public class Instance implements OperationProcessor{
    static Logger log = LoggerFactory.getLogger(Instance.class);
    private Cheat instanceCheat;
    private int identity;
    private int scanCount = 0;
    private boolean found = false;
    private boolean complete = false;

    @Override
    public void process(ArraySearchResultList resultList, long pos, Memory mem) {
    }

    private void filterResults(ArraySearchResultList originalResults, ArraySearchResultList otherResults) {
        complete = true;
        List<ArraySearchResult> results = otherResults.getAllValidList();
        for (ArraySearchResult res : results) {
            for (ArraySearchResult mRes : originalResults.getAllList()) {
                mRes.setValid(false);
                if (mRes.getAddress() == res.getAddress()) {
                    mRes.setValid(true);
                    break;
                }
            }
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void searchComplete(ArraySearchResultList resultList) {
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
                    filterResults(resultList, instanceCheat.getResults());
                }
            }
            else {
                filterResults(resultList, instanceCheat.getResults());
            }
        }
        if (complete) {
            log.trace("Instance Found valid address at {}", FormatTools.valueToHex(resultList.getAllValidList().get(0).getAddress()));
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
