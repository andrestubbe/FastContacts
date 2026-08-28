package fastcontacts;

import java.util.*;

/**
 * High-speed builder and specification for filtering and searching contacts in FastContactsRegistry.
 */
public final class ContactSearchQuery {
    private final String textQuery;
    private final String category;
    private final String emailQuery;
    private final String phoneQuery;
    private final String organization;
    private final int limit;
    private final int offset;
    private final SortField sortField;
    private final boolean ascending;

    public enum SortField {
        FORMATTED_NAME,
        FAMILY_NAME,
        GIVEN_NAME,
        ORGANIZATION,
        REVISION
    }

    private ContactSearchQuery(Builder builder) {
        this.textQuery = builder.textQuery != null ? builder.textQuery.trim() : null;
        this.category = builder.category != null ? builder.category.trim() : null;
        this.emailQuery = builder.emailQuery != null ? builder.emailQuery.trim() : null;
        this.phoneQuery = builder.phoneQuery != null ? builder.phoneQuery.trim() : null;
        this.organization = builder.organization != null ? builder.organization.trim() : null;
        this.limit = builder.limit > 0 ? builder.limit : Integer.MAX_VALUE;
        this.offset = Math.max(0, builder.offset);
        this.sortField = builder.sortField != null ? builder.sortField : SortField.FORMATTED_NAME;
        this.ascending = builder.ascending;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTextQuery() { return textQuery; }
    public String getCategory() { return category; }
    public String getEmailQuery() { return emailQuery; }
    public String getPhoneQuery() { return phoneQuery; }
    public String getOrganization() { return organization; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public SortField getSortField() { return sortField; }
    public boolean isAscending() { return ascending; }

    public static final class Builder {
        private String textQuery;
        private String category;
        private String emailQuery;
        private String phoneQuery;
        private String organization;
        private int limit = Integer.MAX_VALUE;
        private int offset = 0;
        private SortField sortField = SortField.FORMATTED_NAME;
        private boolean ascending = true;

        public Builder textQuery(String text) { this.textQuery = text; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder emailQuery(String email) { this.emailQuery = email; return this; }
        public Builder phoneQuery(String phone) { this.phoneQuery = phone; return this; }
        public Builder organization(String org) { this.organization = org; return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }
        public Builder offset(int offset) { this.offset = offset; return this; }
        public Builder sortBy(SortField sortField, boolean ascending) {
            this.sortField = sortField;
            this.ascending = ascending;
            return this;
        }

        public ContactSearchQuery build() {
            return new ContactSearchQuery(this);
        }
    }
}
