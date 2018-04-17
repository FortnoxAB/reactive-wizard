package se.fortnox.reactivewizard;

public class CollectionOptions {
    public enum SortOrder {
        ASC,
        DESC
    }

    private Integer   limit;
    private Integer   offset;
    private String    sortBy;
    private SortOrder order;

    public CollectionOptions() {
    }

    public CollectionOptions(Integer limit, Integer offset) {
        this(limit, offset, null, null);
    }

    public CollectionOptions(String sortBy, SortOrder order) {
        this(null, null, sortBy, order);
    }

    public CollectionOptions(Integer limit, Integer offset, String sortBy, SortOrder order) {
        this.limit = limit;
        this.offset = offset;
        this.sortBy = sortBy;
        this.order = order;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getOrder() {
        return order;
    }

    public void setOrder(SortOrder order) {
        this.order = order;
    }
}
