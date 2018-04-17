package se.fortnox.reactivewizard.db.paging;

import se.fortnox.reactivewizard.CollectionOptions;

public class CollectionOptionsWithResult extends CollectionOptions {

    private boolean lastRecord = true;

    public CollectionOptionsWithResult(Integer limit, Integer offset) {
        super(limit, offset);
    }

    public CollectionOptionsWithResult(Integer limit, Integer offset, String sortBy, SortOrder order) {
        super(limit, offset, sortBy, order);
    }

    public boolean isLastRecord() {
        return lastRecord;
    }

    public void setNotLastRecord() {
        lastRecord = false;
    }
}
