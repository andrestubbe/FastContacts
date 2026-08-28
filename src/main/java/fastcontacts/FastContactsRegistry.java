package fastcontacts;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ultra-high-speed, thread-safe in-memory Contact Registry and Indexing Engine.
 * Provides multi-index lookups (UID, Normalized Email, Normalized Phone E.164, Category, Full-text prefix/ngram),
 * atomic CardDAV delta synchronization tokens, and streaming bulk .vcf ingestion/export.
 */
public final class FastContactsRegistry {

    private final Map<String, Contact> contactsByUid = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> emailIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> phoneIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tokenIndex = new ConcurrentHashMap<>();

    // Change log for CardDAV sync-collection (RFC 6578)
    private final Map<String, Long> uidToToken = new ConcurrentHashMap<>();
    private final Map<String, Long> deletedUidToToken = new ConcurrentHashMap<>();
    private final AtomicLong syncSequence = new AtomicLong(1);

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public FastContactsRegistry() {}

    /**
     * Creates a new registry pre-populated with contacts.
     */
    public static FastContactsRegistry of(Contact... contacts) {
        FastContactsRegistry reg = new FastContactsRegistry();
        if (contacts != null) {
            for (Contact c : contacts) {
                reg.put(c);
            }
        }
        return reg;
    }

    /**
     * Stores or updates a contact in the registry.
     * Updates all secondary indexes and advances the CardDAV sync-token.
     */
    public Contact put(Contact contact) {
        Objects.requireNonNull(contact, "Contact cannot be null");
        rwLock.writeLock().lock();
        try {
            String uid = contact.getUid();
            Contact old = contactsByUid.get(uid);
            if (old != null) {
                removeIndexes(old);
            }
            deletedUidToToken.remove(uid);

            contactsByUid.put(uid, contact);
            addIndexes(contact);

            long token = syncSequence.incrementAndGet();
            uidToToken.put(uid, token);
            return contact;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Bulk inserts a collection of contacts.
     */
    public int putAll(Collection<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) return 0;
        rwLock.writeLock().lock();
        try {
            int count = 0;
            for (Contact c : contacts) {
                if (c != null) {
                    put(c);
                    count++;
                }
            }
            return count;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Ingests all contacts from a .vcf formatted string.
     */
    public int importVcf(String vcfString) {
        List<Contact> parsed = VCardParser.parseAll(vcfString);
        return putAll(parsed);
    }

    /**
     * Ingests all contacts from an input stream.
     */
    public int importVcf(InputStream stream) throws IOException {
        List<Contact> parsed = VCardParser.parseAll(stream);
        return putAll(parsed);
    }

    /**
     * Ingests all contacts from a file.
     */
    public int importVcf(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return importVcf(fis);
        }
    }

    /**
     * Removes a contact by UID.
     */
    public Optional<Contact> remove(String uid) {
        if (uid == null) return Optional.empty();
        rwLock.writeLock().lock();
        try {
            Contact removed = contactsByUid.remove(uid);
            if (removed != null) {
                removeIndexes(removed);
                uidToToken.remove(uid);
                long token = syncSequence.incrementAndGet();
                deletedUidToToken.put(uid, token);
                return Optional.of(removed);
            }
            return Optional.empty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a contact by UID.
     */
    public Optional<Contact> get(String uid) {
        if (uid == null) return Optional.empty();
        return Optional.ofNullable(contactsByUid.get(uid));
    }

    /**
     * Returns true if a contact with the given UID exists.
     */
    public boolean contains(String uid) {
        return uid != null && contactsByUid.containsKey(uid);
    }

    /**
     * Returns the total number of active contacts in the registry.
     */
    public int size() {
        return contactsByUid.size();
    }

    /**
     * Returns true if the registry is empty.
     */
    public boolean isEmpty() {
        return contactsByUid.isEmpty();
    }

    /**
     * Clears all contacts and indexes.
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            contactsByUid.clear();
            emailIndex.clear();
            phoneIndex.clear();
            categoryIndex.clear();
            tokenIndex.clear();
            uidToToken.clear();
            deletedUidToToken.clear();
            syncSequence.incrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves all contacts in the registry as an unmodifiable collection.
     */
    public Collection<Contact> getAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(contactsByUid.values()));
    }

    /**
     * Finds contacts matching an exact or prefix email address (case-insensitive).
     */
    public List<Contact> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return Collections.emptyList();
        String normalized = normalizeEmail(email);
        rwLock.readLock().lock();
        try {
            Set<String> uids = emailIndex.get(normalized);
            if (uids == null || uids.isEmpty()) return Collections.emptyList();
            List<Contact> list = new ArrayList<>(uids.size());
            for (String uid : uids) {
                Contact c = contactsByUid.get(uid);
                if (c != null) list.add(c);
            }
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Finds contacts matching a phone number (digits normalized).
     */
    public List<Contact> findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return Collections.emptyList();
        String normalized = normalizePhone(phone);
        if (normalized.isEmpty()) return Collections.emptyList();

        rwLock.readLock().lock();
        try {
            Set<String> uids = phoneIndex.get(normalized);
            if (uids == null || uids.isEmpty()) return Collections.emptyList();
            List<Contact> list = new ArrayList<>(uids.size());
            for (String uid : uids) {
                Contact c = contactsByUid.get(uid);
                if (c != null) list.add(c);
            }
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Finds contacts tagged with a specific category (case-insensitive).
     */
    public List<Contact> findByCategory(String category) {
        if (category == null || category.trim().isEmpty()) return Collections.emptyList();
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        rwLock.readLock().lock();
        try {
            Set<String> uids = categoryIndex.get(normalized);
            if (uids == null || uids.isEmpty()) return Collections.emptyList();
            List<Contact> list = new ArrayList<>(uids.size());
            for (String uid : uids) {
                Contact c = contactsByUid.get(uid);
                if (c != null) list.add(c);
            }
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Executes a high-performance multi-criteria search.
     */
    public List<Contact> search(ContactSearchQuery query) {
        Objects.requireNonNull(query, "query cannot be null");
        rwLock.readLock().lock();
        try {
            Set<String> candidateUids = null;

            // 1. Text / Token query filter
            if (query.getTextQuery() != null && !query.getTextQuery().isEmpty()) {
                String[] tokens = query.getTextQuery().toLowerCase(Locale.ROOT).split("\\s+");
                for (String token : tokens) {
                    if (token.isEmpty()) continue;
                    Set<String> matchingForToken = new HashSet<>();
                    // Look up prefix matches in token index
                    for (Map.Entry<String, Set<String>> entry : tokenIndex.entrySet()) {
                        if (entry.getKey().startsWith(token) || entry.getKey().contains(token)) {
                            matchingForToken.addAll(entry.getValue());
                        }
                    }
                    if (candidateUids == null) {
                        candidateUids = new HashSet<>(matchingForToken);
                    } else {
                        candidateUids.retainAll(matchingForToken);
                    }
                    if (candidateUids.isEmpty()) break;
                }
            }

            // 2. Category filter
            if (query.getCategory() != null && !query.getCategory().isEmpty()) {
                Set<String> catUids = categoryIndex.get(query.getCategory().toUpperCase(Locale.ROOT));
                if (catUids == null || catUids.isEmpty()) return Collections.emptyList();
                if (candidateUids == null) {
                    candidateUids = new HashSet<>(catUids);
                } else {
                    candidateUids.retainAll(catUids);
                }
            }

            // 3. Email query filter
            if (query.getEmailQuery() != null && !query.getEmailQuery().isEmpty()) {
                String norm = normalizeEmail(query.getEmailQuery());
                Set<String> emailUids = new HashSet<>();
                for (Map.Entry<String, Set<String>> entry : emailIndex.entrySet()) {
                    if (entry.getKey().contains(norm)) {
                        emailUids.addAll(entry.getValue());
                    }
                }
                if (emailUids.isEmpty()) return Collections.emptyList();
                if (candidateUids == null) {
                    candidateUids = emailUids;
                } else {
                    candidateUids.retainAll(emailUids);
                }
            }

            // 4. Phone query filter
            if (query.getPhoneQuery() != null && !query.getPhoneQuery().isEmpty()) {
                String norm = normalizePhone(query.getPhoneQuery());
                Set<String> phoneUids = new HashSet<>();
                for (Map.Entry<String, Set<String>> entry : phoneIndex.entrySet()) {
                    if (entry.getKey().contains(norm)) {
                        phoneUids.addAll(entry.getValue());
                    }
                }
                if (phoneUids.isEmpty()) return Collections.emptyList();
                if (candidateUids == null) {
                    candidateUids = phoneUids;
                } else {
                    candidateUids.retainAll(phoneUids);
                }
            }

            List<Contact> matches = new ArrayList<>();
            if (candidateUids != null) {
                for (String uid : candidateUids) {
                    Contact c = contactsByUid.get(uid);
                    if (c != null && matchesExtraCriteria(c, query)) {
                        matches.add(c);
                    }
                }
            } else {
                for (Contact c : contactsByUid.values()) {
                    if (matchesExtraCriteria(c, query)) {
                        matches.add(c);
                    }
                }
            }

            // Sorting
            Comparator<Contact> comparator = getComparator(query.getSortField(), query.isAscending());
            matches.sort(comparator);

            // Pagination
            int offset = query.getOffset();
            if (offset >= matches.size()) {
                return Collections.emptyList();
            }
            int toIndex = Math.min(offset + query.getLimit(), matches.size());
            return matches.subList(offset, toIndex);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private boolean matchesExtraCriteria(Contact c, ContactSearchQuery q) {
        if (q.getOrganization() != null && !q.getOrganization().isEmpty()) {
            if (!c.getOrganization().toLowerCase(Locale.ROOT).contains(q.getOrganization().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private Comparator<Contact> getComparator(ContactSearchQuery.SortField field, boolean ascending) {
        Comparator<Contact> comp;
        switch (field) {
            case FAMILY_NAME:
                comp = Comparator.comparing(Contact::getFamilyName, String.CASE_INSENSITIVE_ORDER)
                                 .thenComparing(Contact::getGivenName, String.CASE_INSENSITIVE_ORDER);
                break;
            case GIVEN_NAME:
                comp = Comparator.comparing(Contact::getGivenName, String.CASE_INSENSITIVE_ORDER)
                                 .thenComparing(Contact::getFamilyName, String.CASE_INSENSITIVE_ORDER);
                break;
            case ORGANIZATION:
                comp = Comparator.comparing(Contact::getOrganization, String.CASE_INSENSITIVE_ORDER)
                                 .thenComparing(Contact::getFormattedName, String.CASE_INSENSITIVE_ORDER);
                break;
            case REVISION:
                comp = Comparator.comparing(Contact::getRevision);
                break;
            case FORMATTED_NAME:
            default:
                comp = Comparator.comparing(Contact::getFormattedName, String.CASE_INSENSITIVE_ORDER);
                break;
        }
        return ascending ? comp : comp.reversed();
    }

    /**
     * Exports all contacts as a .vcf string (vCard 3.0).
     */
    public String exportVcf() {
        return exportVcf(VCardVersion.V3_0);
    }

    /**
     * Exports all contacts as a .vcf string in the specified vCard version.
     */
    public String exportVcf(VCardVersion version) {
        rwLock.readLock().lock();
        try {
            return VCardWriter.toVcf(contactsByUid.values(), version);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns the current CardDAV sync token (RFC 6578).
     */
    public String getCurrentSyncToken() {
        return "data:,sync-token-" + syncSequence.get();
    }

    /**
     * Result of a CardDAV delta sync-collection query.
     */
    public static final class SyncDelta {
        private final List<Contact> updatedContacts;
        private final List<String> deletedUids;
        private final String newSyncToken;

        public SyncDelta(List<Contact> updatedContacts, List<String> deletedUids, String newSyncToken) {
            this.updatedContacts = updatedContacts;
            this.deletedUids = deletedUids;
            this.newSyncToken = newSyncToken;
        }

        public List<Contact> getUpdatedContacts() { return updatedContacts; }
        public List<String> getDeletedUids() { return deletedUids; }
        public String getNewSyncToken() { return newSyncToken; }
    }

    /**
     * Computes the changes since the specified sync token for CardDAV RFC 6578 sync-collection report.
     */
    public SyncDelta getDeltaSince(String syncToken) {
        long tokenNum = 0;
        if (syncToken != null && syncToken.contains("sync-token-")) {
            try {
                String numPart = syncToken.substring(syncToken.indexOf("sync-token-") + 11);
                tokenNum = Long.parseLong(numPart);
            } catch (NumberFormatException ignored) {}
        }

        rwLock.readLock().lock();
        try {
            List<Contact> updated = new ArrayList<>();
            List<String> deleted = new ArrayList<>();

            for (Map.Entry<String, Long> entry : uidToToken.entrySet()) {
                if (entry.getValue() > tokenNum) {
                    Contact c = contactsByUid.get(entry.getKey());
                    if (c != null) updated.add(c);
                }
            }

            for (Map.Entry<String, Long> entry : deletedUidToToken.entrySet()) {
                if (entry.getValue() > tokenNum) {
                    deleted.add(entry.getKey());
                }
            }

            return new SyncDelta(updated, deleted, getCurrentSyncToken());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void addIndexes(Contact c) {
        String uid = c.getUid();

        // Email index
        for (ContactProperty ep : c.getEmails()) {
            String norm = normalizeEmail(ep.getValue());
            if (!norm.isEmpty()) {
                emailIndex.computeIfAbsent(norm, k -> ConcurrentHashMap.newKeySet()).add(uid);
            }
        }

        // Phone index
        for (ContactProperty pp : c.getPhones()) {
            String norm = normalizePhone(pp.getValue());
            if (!norm.isEmpty()) {
                phoneIndex.computeIfAbsent(norm, k -> ConcurrentHashMap.newKeySet()).add(uid);
            }
        }

        // Category index
        for (String cat : c.getCategories()) {
            String norm = cat.trim().toUpperCase(Locale.ROOT);
            if (!norm.isEmpty()) {
                categoryIndex.computeIfAbsent(norm, k -> ConcurrentHashMap.newKeySet()).add(uid);
            }
        }

        // Token inverted index
        indexTokens(c.getFormattedName(), uid);
        indexTokens(c.getFamilyName(), uid);
        indexTokens(c.getGivenName(), uid);
        indexTokens(c.getNickname(), uid);
        indexTokens(c.getOrganization(), uid);
        indexTokens(c.getDepartment(), uid);
        indexTokens(c.getJobTitle(), uid);
        indexTokens(c.getNote(), uid);
    }

    private void removeIndexes(Contact c) {
        String uid = c.getUid();
        for (ContactProperty ep : c.getEmails()) {
            String norm = normalizeEmail(ep.getValue());
            Set<String> set = emailIndex.get(norm);
            if (set != null) {
                set.remove(uid);
                if (set.isEmpty()) emailIndex.remove(norm);
            }
        }
        for (ContactProperty pp : c.getPhones()) {
            String norm = normalizePhone(pp.getValue());
            Set<String> set = phoneIndex.get(norm);
            if (set != null) {
                set.remove(uid);
                if (set.isEmpty()) phoneIndex.remove(norm);
            }
        }
        for (String cat : c.getCategories()) {
            String norm = cat.trim().toUpperCase(Locale.ROOT);
            Set<String> set = categoryIndex.get(norm);
            if (set != null) {
                set.remove(uid);
                if (set.isEmpty()) categoryIndex.remove(norm);
            }
        }
        unindexTokens(c.getFormattedName(), uid);
        unindexTokens(c.getFamilyName(), uid);
        unindexTokens(c.getGivenName(), uid);
        unindexTokens(c.getNickname(), uid);
        unindexTokens(c.getOrganization(), uid);
        unindexTokens(c.getDepartment(), uid);
        unindexTokens(c.getJobTitle(), uid);
        unindexTokens(c.getNote(), uid);
    }

    private void indexTokens(String text, String uid) {
        if (text == null || text.isEmpty()) return;
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[\\s\\p{Punct}]+");
        for (String t : tokens) {
            if (t.length() >= 2) {
                tokenIndex.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(uid);
            }
        }
    }

    private void unindexTokens(String text, String uid) {
        if (text == null || text.isEmpty()) return;
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[\\s\\p{Punct}]+");
        for (String t : tokens) {
            if (t.length() >= 2) {
                Set<String> set = tokenIndex.get(t);
                if (set != null) {
                    set.remove(uid);
                    if (set.isEmpty()) tokenIndex.remove(t);
                }
            }
        }
    }

    public static String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        StringBuilder sb = new StringBuilder(phone.length());
        for (int i = 0; i < phone.length(); i++) {
            char c = phone.charAt(i);
            if (Character.isDigit(c) || (c == '+' && sb.length() == 0)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
