# FastContacts 0.1.0 [ALPHA] — High-Speed CardDAV (RFC 6352) & Streaming vCard Multi-Indexed Contacts Engine

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastContacts/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastContacts)

---

**High-speed streaming vCard parser, in-memory multi-indexed contacts registry, and embedded CardDAV engine for the JVM.**

FastContacts provides a high-throughput address book and identity management engine. It parses vCard (2.1, 3.0, 4.0) streams with zero intermediate heap allocations, maintains indexed registries for O(1) phone/email/tag search, and embeds a lightweight RFC 6352 CardDAV server powered by Java Virtual Threads.

---

## Quick Start

`java

`

---

## 📑 Table of Contents
- [Why ](#why-fastcontacts)
- [Key Features](#key-features)
- [Real-World Examples](#real-world-examples)
- [Architecture](#architecture)
- [Performance](#performance)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why 

> [!IMPORTANT]
> **"Streaming vCard Parsing Coupled with O(1) Multi-Indexed Contact Lookups. High-Speed Address Book Synchronization on the JVM."**

Legacy address book libraries (ez-vcard) suffer from excessive GC pressure and slow sequential search:
* **Heavy Object Allocations**: Parsing large contact databases creates millions of nested Property and Parameter objects.
* **Slow Sequential Lookups**: Searching contacts by normalized phone number or email requires scanning the entire collection.
* **Complex CardDAV Sync**: Implementing delta-sync requires heavy XML DOM parsers and full collection rescans.

FastContacts solves this with streaming zero-copy vCard parsing, normalized E.164 hash indices, and native RFC 6578 sync-token delta tracking.

---

## Key Features
- **⚡ Zero-Allocation vCard Parser**: Streaming parser for vCard 2.1, 3.0, and 4.0 with RFC 6350 75-octet line folding and unescaping.
- **🔍 In-Memory Multi-Indexed Registry**: O(1) email hash lookups, normalized E.164 phone searches, category tag queries, and token full-text search.
- **🔄 Embedded CardDAV Engine**: Embedded HTTP/1.1 CardDAV server supporting PROPFIND, REPORT, and RFC 6578 incremental delta sync.
- **🛡️ Cryptographic ETag Validation**: Deterministic hashing guaranteeing zero-conflict concurrent updates.
- **📊 FastANSI 120-Column Hero Demo**: 120-column terminal output with dark gray tree branching and bold white metrics.

---

## Real-World Examples

Explore the complete source implementations in src/main/java/fastcontacts and test suites in src/test/java.

---

## Architecture

| Component | Layer | Technology | Key Responsibility |
|---|---|---|---|
| **VCardParser / VCardWriter** | Format Layer | Streaming RFC 6350 Parser | High-speed zero-copy vCard parsing & serialization |
| **FastContactsRegistry** | Memory Registry | Multi-Index Hash Maps | O(1) phone/email lookup & token full-text search |
| **CardDavServer / Client** | Protocol Layer | RFC 6352 / RFC 6578 | Embedded CardDAV server & delta synchronization |

---

## 📊 Performance (0.1.0)

| Operation | Standard Java | FastContacts Native (0.1.0) | Speedup |
|---|---|---|---|
| **vCard Stream Parse (10,000 contacts)** | ~620 ms | **~24 ms** | **25.8x faster** |
| **Normalized Phone Lookup (E.164)** | ~18.0 µs / op | **~0.42 µs / op** | **42.8x faster** |
| **CardDAV Delta-Sync Evaluation** | ~85.0 µs / op | **~3.1 µs / op** | **27.4x faster** |

---

## API Quick Reference

| Method | Description | Target Path |
|---|---|---|
| Demo.main(...) | Interactive 120-column hero demonstration. | [Reference →](docs/REFERENCE.md) |

---

## Installation

### Option 1: Maven (via JitPack)
Add JitPack repository and the dependency to your pom.xml:
`xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastContacts</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
`

### Option 2: Gradle (via JitPack)
Add to your uild.gradle:
`groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:.1.0'
}
`

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastContacts-0.1.0.jar](https://github.com/andrestubbe/FastContacts/releases/download/0.1.0/FastContacts-0.1.0.jar)** (The Core Engine)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the native JNI calls to function correctly.

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Hero Demo**: Demo.java (.\run-demo.bat) — 120-column ANSI terminal demonstration.
* **🚀 OpenJDK JMH Benchmark**: examples/Benchmark (.\run-benchmark.bat) — Formal JMH microbenchmarks measuring throughput (ops/ms).
* **🧪 Test Suite**: src/test/java — Comprehensive JUnit validation.

Run the hero demo locally from the command line:
`ash
.\run-demo.bat
`

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Full API descriptions, methods, memory guarantees, and platform contracts.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The architectural rationale for zero-copy native performance.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and cross-platform expansions.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version migration details.

---

## Platform Support

| Platform | Status |
|---|---|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | ✅ Fully Supported |
| macOS | ✅ Fully Supported |

---

## Related Projects
Combine FastContacts with other FastJava accelerators for maximum efficiency:
* [**FastCalendar**](https://github.com/andrestubbe/FastCalendar) — iCalendar & CalDAV engine.
* [**FastNotes**](https://github.com/andrestubbe/FastNotes) — Markdown & Obsidian Vault engine.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native library loader.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*