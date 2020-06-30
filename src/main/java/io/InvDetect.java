package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class InvDetect extends Detect {
    static Logger log = LoggerFactory.getLogger(InvDetect.class);

    @Override
    public void searchComplete(Collection<ArraySearchResult> results) {
        List<ArraySearchResult> validResults = results.stream().filter(ArraySearchResult::isValid).collect(Collectors.toList());
        boolean found = validResults.stream().anyMatch(e ->
                e.getMiscData() != null &&
                        e.getMiscData() instanceof DetectData &&
                        ((DetectData) e.getMiscData()).getDetectRange() == DetectData.DetectRange.INRANGE);
        if (found) {
            for (ArraySearchResult res : validResults) {
                if (res.getMiscData() instanceof DetectData) {
                    res.setValid(((DetectData)res.getMiscData()).getDetectRange() != DetectData.DetectRange.INRANGE);
                }
            }
        }
        complete = found;
    }

}
