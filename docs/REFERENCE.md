# FastContacts API & Architecture Reference

`FastContacts` provides high-speed vCard (2.1, 3.0, 4.0) parsing, serialization, in-memory multi-indexing, and an embedded RFC 6352 CardDAV server.

---

## 1. Core Model & Records

### `Contact`
Immutable, thread-safe representation of a contact entity.

```java
Contact contact = Contact.builder()
    .uid("c-101")
    .formattedName("Linus Torvalds")
    .givenName("Linus")
    .familyName("Torvalds")
    .organization("Linux Foundation")
    .jobTitle("Principal Fellow")
    .addEmail("torvalds@linux-foundation.org", "WORK", "PREF")
    .addPhone("+1 503 555 0199", "WORK", "CELL")
    .addAddress(ContactAddress.of("100 SW Main St", "Portland", "OR", "97204", "USA", "WORK"))
    .addCategory("Kernel")
    .note("Creator of Linux and Git.")
    .build();
```

### Key Methods on `Contact`:
* `getUid()`: Unique persistent identifier.
* `getFormattedName()`: Display name (`FN`).
* `getPrimaryEmail()`: Returns preferred or first email.
* `getPrimaryPhone()`: Returns preferred or first phone number.
* `getPrimaryAddress()`: Returns first postal address.
* `getEtag()`: Cryptographic hash ETag for HTTP/CardDAV cache & concurrency validation.
* `toBuilder()`: Creates a mutable copy builder for updating fields.

---

## 2. In-Memory Registry (`FastContactsRegistry`)

High-throughput, thread-safe contacts container with automated multi-indexing and CardDAV sync-token tracking.

```java
FastContactsRegistry registry = new FastContactsRegistry();

// Insert / Update
registry.put(contact);

// Bulk Ingestion
registry.importVcf(vcfStringOrStream);

// O(1) Lookups
List<Contact> byEmail = registry.findByEmail("torvalds@linux-foundation.org");
List<Contact> byPhone = registry.findByPhone("+15035550199"); // E.164 normalized
List<Contact> byCat   = registry.findByCategory("Kernel");

// Full-Text Multi-Field Search
List<Contact> results = registry.search(ContactSearchQuery.builder()
    .textQuery("Linux Fellow")
    .category("Kernel")
    .limit(50)
    .sortBy(ContactSearchQuery.SortField.FAMILY_NAME, true)
    .build());

// CardDAV Delta Sync (RFC 6578)
FastContactsRegistry.SyncDelta delta = registry.getDeltaSince(clientSyncToken);
List<Contact> updated = delta.getUpdatedContacts();
List<String> deleted  = delta.getDeletedUids();
String nextToken      = delta.getNewSyncToken();
```

---

## 3. Streaming vCard Parser & Writer (`VCardParser`, `VCardWriter`)

### Streaming Parsing
```java
// Single Contact
Contact c = VCardParser.parse(vcardText);

// Bulk .vcf File / Stream
List<Contact> contacts = VCardParser.parseAll(new FileInputStream("contacts.vcf"));
```

### Formatting & Export
```java
// vCard 3.0 (RFC 2426)
String vcard3 = VCardWriter.toVCard(contact, VCardVersion.V3_0);

// vCard 4.0 (RFC 6350)
String vcard4 = VCardWriter.toVCard(contact, VCardVersion.V4_0);

// Bulk .vcf Export
String vcf = registry.exportVcf();
```

---

## 4. Embedded CardDAV Server (`CardDavServer`)

Lightweight, high-performance embedded HTTP/1.1 CardDAV server supporting virtual threads and zero external dependencies.

```java
FastContactsRegistry registry = new FastContactsRegistry();
// Seed contacts...

try (CardDavServer server = new CardDavServer(8080, registry)) {
    server.start();
    System.out.println("CardDAV endpoint ready: " + server.getBaseUrl());
    // macOS / iOS / Thunderbird can connect to http://localhost:8080/addressbooks/default/
}
```

### Supported HTTP Methods & Protocols:
* `OPTIONS`: Advertises DAV Level 1, 2, 3, addressbook, access-control, sync-collection.
* `PROPFIND`: Addressbook collection discovery and sync-token reporting.
* `REPORT`:
  * `addressbook-query`: Multi-property vCard querying.
  * `addressbook-multiget`: Batch retrieval of specific contact hrefs.
  * `sync-collection` (RFC 6578): Incremental delta synchronization with change tracking.
* `GET`: Downloads `.vcf` single or bulk addressbook.
* `PUT`: Uploads or modifies contacts with `If-Match` optimistic locking.
* `DELETE`: Deletes contacts and registers tombstone tokens for sync clients.

---

## 5. FastANSI 120-Column HUD (`FastAnsiContactsHUD`)

Terminal formatter designed for operational monitoring, CLI tools, and hero demos:
* `printBanner(title, subtitle)`: 120-column boxed header.
* `printSection(sectionTitle)`: Boxed divider.
* `printContactCard(contact, index, total)`: Formatted tree card with dark gray branches, bold white values, and middle-path truncation.
* `printMetricRow(label, value, unit, badge)`: Performance dashboard metric row.
