package io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.ArraySearchResult;
import script.ArraySearchResultList;

public class InvDetect extends Detect {
    static Logger log = LoggerFactory.getLogger(InvDetect.class);

    @Override
    public void searchComplete(ArraySearchResultList resultList) {
        boolean found = resultList.getAllValidList().stream().anyMatch(e ->
                e.getMiscData() != null &&
                        e.getMiscData() instanceof DetectData &&
                        ((DetectData) e.getMiscData()).getDetectRange() == DetectData.DetectRange.INRANGE);
        if (found) {
            for (ArraySearchResult res : resultList.getAllValidList()) {
                if (res.getMiscData() instanceof DetectData) {
                    res.setValid(((DetectData)res.getMiscData()).getDetectRange() != DetectData.DetectRange.INRANGE);
                }
            }
        }
        complete = found && resultList.getAllValidList().size() > 0;
    }

}
