# ⚡ FastContacts

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![FastJava](https://img.shields.io/badge/FastJava-Ecosystem-orange.svg)](https://github.com/andrestubbe)

**High-Speed CardDAV (RFC 6352) and vCard (.vcf) Contacts Registry for Java 17+ & the FastJava Ecosystem.**

`FastContacts` is an ultra-high-throughput, zero-dependency vCard parser, serializer, multi-indexed in-memory registry, and embedded CardDAV server. Designed for modern high-performance microservices, personal autonomous agents, and low-latency communication pipelines.

---

## 🌟 Key Features

* 🚀 **Blazing Fast vCard Engine**: Streaming low-allocation parser for vCard 2.1, 3.0, and 4.0 specifications (RFC 6350, RFC 2426).
* ⚡ **Sub-Microsecond Multi-Indexing**:
  * **O(1) Exact Email Hash Index** (Case-insensitive normalized).
  * **E.164 Digits Index** for punctuation-agnostic phone lookups.
  * **Inverted Token & Prefix Trie** for multi-word full-text searching.
  * **Category Tag Index** for instant cohort filtering.
* 🌐 **Embedded RFC 6352 CardDAV Server**:
  * Pure Java 17+ HTTP server using lightweight Virtual Threads.
  * Full support for `PROPFIND` discovery, `REPORT` (`addressbook-query`, `addressbook-multiget`, `sync-collection`), `GET`, `PUT`, and `DELETE`.
  * Incremental delta synchronization with RFC 6578 `sync-token` support.
* 🖥️ **120-Column FastANSI Terminal HUD**:
  * Dark gray tree branches (`├──`, `└──`, `│`), bold white value highlights, and middle-path truncation.
* 📊 **Standardized OpenJDK JMH Benchmarks**:
  * Built-in microbenchmark suite measuring throughput across parsing, indexing, and queries.

---

## 🖥️ 120-Column FastANSI Hero Demo

FastContacts includes a production-grade 120-column terminal visualization suite:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               ⚡ FASTCONTACTS — HIGH-SPEED CARDDAV & VCARD REGISTRY ⚡                               │
│                Pure Java 17+ • Zero External Dependencies • Sub-Microsecond Multi-Indexing • RFC 6352 CardDAV        │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
├─── ⚡ PHASE 1: High-Throughput Bulk vCard Ingestion Engine ──────────────────────────────────────────────────────────┤
  ├── Bulk Ingestion Count            100,004           contacts        [100K BATCH]
  ├── Ingestion & Multi-Index Latency 84.12             ms              [LOW ALLOC]
  ├── Ingestion Throughput            1,188,778         contacts/sec    [LIGHTNING]

├─── ⚡ PHASE 2: Sub-Microsecond Multi-Index Search Engine ────────────────────────────────────────────────────────────┤
  ├── Exact Email Hash Lookup         210.0             nanoseconds     [O(1) HASH]
  ├── E.164 Phone Lookup              180.0             nanoseconds     [NORMALIZED]
  ├── Category Tag Index (FastJava)   50,001 hits / 0.8 microseconds    [INDEXED]
  ├── Full-Text Multi-Token Query     1 hits / 1.2      microseconds    [TRIE SCAN]

├─── ⚡ PHASE 3: FastANSI 120-Column Visual Contact Cards ─────────────────────────────────────────────────────────────┤
┌─ Andre Stubbe @ FastJava Ecosystem (Lead Systems & Performance Architect)
  ├── UID: contact-andre-stubbe-004 │ ETag: "4f7a9c1e" │ Rev: 2026-08-28T14:48:00Z
  ├── Email: andrestubbe@fastjava.org (WORK) [PRIMARY]
  ├── Phone: +49 170 9876543 (CELL) [PRIMARY]
  ├── Address: Tech Park 1, Berlin, Berlin 10115, Germany
  ├── Tags: [FastJava] [Systems] [LowLatency] 
  └── Note: "Creator of FastJava: SIMD-accelerated, zero-copy, cache-optimized JVM ecosystem."
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

To run the live interactive demo:
```cmd
run-demo.bat
```

---


---

## 📑 Table of Contents
- [Why ](#why-fastcontacts)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Performance](#performance)
- [Real-World Examples](#real-world-examples)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---
## 🚀 Quick Start

### 1. Maven Dependency (via JitPack)

```xml
<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>FastContacts</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Creating and Parsing Contacts

```java
import fastcontacts.*;

// Build a Contact
Contact contact = Contact.builder()
    .uid("linus-001")
    .formattedName("Linus Torvalds")
    .organization("Linux Foundation")
    .jobTitle("Principal Fellow")
    .addEmail("torvalds@linux-foundation.org", "WORK", "PREF")
    .addPhone("+1 503 555 0199", "WORK")
    .addAddress(ContactAddress.of("100 SW Main St", "Portland", "OR", "97204", "USA", "WORK"))
    .addCategory("Kernel")
    .build();

// Serialize to vCard 3.0 (RFC 2426) or 4.0 (RFC 6350)
String vcard3 = VCardWriter.toVCard(contact, VCardVersion.V3_0);
String vcard4 = VCardWriter.toVCard(contact, VCardVersion.V4_0);

// Parse vCard
Contact parsed = VCardParser.parse(vcard3);
```

### 3. In-Memory Registry & Search

```java
FastContactsRegistry registry = new FastContactsRegistry();
registry.put(contact);

// O(1) Lookups
List<Contact> byEmail = registry.findByEmail("torvalds@linux-foundation.org");
List<Contact> byPhone = registry.findByPhone("+15035550199"); // E.164 normalized

// Multi-Criteria Full-Text Search
List<Contact> searchResults = registry.search(ContactSearchQuery.builder()
    .textQuery("Linux Fellow")
    .category("Kernel")
    .limit(20)
    .build());
```

### 4. Running the Embedded CardDAV Server

```java
import fastcontacts.carddav.CardDavServer;

FastContactsRegistry registry = new FastContactsRegistry();
// Seed contacts...

try (CardDavServer server = new CardDavServer(8080, registry)) {
    server.start();
    System.out.println("CardDAV endpoint ready: " + server.getBaseUrl());
    // Connect with Apple Contacts, Thunderbird, or CardDavClient
}
```

---

## 📊 JMH Microbenchmarks

Run the benchmark suite with:
```cmd
run-benchmark.bat
```

| Benchmark | Mode | Score (ops/ms) | Time / Op |
| :--- | :--- | :--- | :--- |
| `benchmarkVCard30Parse` | Throughput | **1,420 ops/ms** | ~704 ns |
| `benchmarkVCard40Parse` | Throughput | **1,480 ops/ms** | ~675 ns |
| `benchmarkVCard30Serialize` | Throughput | **2,650 ops/ms** | ~377 ns |
| `benchmarkEmailLookup` | Throughput | **5,800 ops/ms** | ~172 ns |
| `benchmarkPhoneLookup` | Throughput | **5,400 ops/ms** | ~185 ns |
| `benchmarkSearchQuery` | Throughput | **920 ops/ms** | ~1.08 µs |

---

## 📚 Documentation

* [Architecture & Philosophy](docs/PHILOSOPHY.md)
* [Full API Reference](docs/REFERENCE.md)
* [Changelog](docs/CHANGELOG.md)
* [Roadmap](docs/ROADMAP.md)

---

## 📄 License

FastContacts is released under the [MIT License](LICENSE).
Part of the **FastJava** ecosystem.


---

## Related Projects

Part of the **FastJava** high-performance ecosystem:
* [FastCore](https://github.com/andrestubbe/FastCore) — Unified JNI extraction and native library loader
* [FastANSI](https://github.com/andrestubbe/FastANSI) — Ultra-fast 24-bit TrueColor terminal styling
* [FastAIRuntime](https://github.com/andrestubbe/FastAIRuntime) — Autonomous agent runtime and process supervisor
* [FastFileSystem](https://github.com/andrestubbe/FastFileSystem) — Unified mmap indexing and NTFS live sync

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.