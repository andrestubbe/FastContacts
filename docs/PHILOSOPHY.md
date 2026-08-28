# The Philosophy of FastContacts

> [!IMPORTANT]
> **"Zero Allocation Bottlenecks. Sub-Microsecond Multi-Indexing. Native-Speed Synchronization."**

FastContacts is built on the fundamental principle that address books and contact registries in modern microservices, personal autonomous agents, CRM platforms, and communication pipelines must operate at hardware-level efficiency.

---

## Core Tenets

### 1. Zero Garbage on Critical Parse Paths
Traditional vCard libraries instantiate thousands of intermediate String, Node, and Map objects per contact, causing massive GC pressure and latency spikes during address book synchronization. FastContacts uses streaming tokenization, low-allocation line unfolding, and pre-sized collections to ingest hundreds of thousands of vCards per second.

### 2. Sub-Microsecond Multi-Indexed Queries
Modern users expect instant search results across names, organizations, phone numbers, and emails. FastContacts maintains concurrent multi-indexes:
* **O(1) Exact Email Hash Index** (Case-insensitive normalized).
* **E.164 Digits Index** for instantaneous phone number matching regardless of regional punctuation.
* **Inverted Token & Prefix Index** for sub-microsecond fuzzy and full-text keyword queries.
* **Category / Tag Bitsets & Sets** for lightning-fast cohort filtering.

### 3. Native CardDAV RFC Compliance Without Framework Bloat
Rather than bundling heavyweight XML application servers, FastContacts implements an embedded, high-throughput CardDAV engine (RFC 6352, RFC 4791, RFC 4918) with native RFC 6578 delta `sync-collection` tokens. This enables direct, seamless synchronization with Apple iOS/macOS Contacts, Thunderbird, Nextcloud, and custom autonomous agents.

### 4. Deterministic Concurrency
Built on Java 17+ with Virtual Thread compatibility and ReentrantReadWriteLock segmented indexing, FastContacts ensures read scalability across hundreds of concurrent threads without locking contention.

---

**⚡ FastContacts — High-Performance Native Contacts Registry for the FastJava Ecosystem.**
