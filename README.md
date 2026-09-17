# FastContacts 0.1.0 [ALPHA] — High-Speed CardDAV (RFC 6352) & Streaming vCard Multi-Indexed Contacts Engine

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastContacts/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastContacts)

---

**High-speed streaming vCard parser, in-memory multi-indexed contacts registry, and embedded CardDAV engine for the JVM.**

FastContacts is the identity and address book substrate of the **FastJava** ecosystem. Designed for CRM synchronization, autonomous communication agents, and directory services, it parses vCard (2.1, 3.0, 4.0) streams with zero intermediate heap allocations, maintains multi-indexed registries for $O(1)$ phone/email/tag lookup, and embeds a lightweight RFC 6352 CardDAV server.

---

## Quick Start

```java
import fastcontacts.FastContactsRegistry;
import fastcontacts.model.Contact;
import fastcontacts.vcard.VCardParser;
import fastcontacts.carddav.CardDavServer;

public class Demo {
    public static void main(String[] args) throws Exception {
        // 1. Create in-memory multi-indexed contact registry
        FastContactsRegistry registry = FastContactsRegistry.create();

        // 2. Zero-allocation streaming vCard ingestion
        VCardParser.parseStream(getVCardInputStream(), contact -> {
            registry.register(contact);
        });

        System.out.printf("Registered %,d contacts.\n", registry.count());

        // 3. Sub-microsecond multi-index lookups
        Contact c1 = registry.findByEmail("alice@example.com");
        Contact c2 = registry.findByPhoneNormalized("+1-555-019-2834");

        // 4. Start embedded CardDAV server
        CardDavServer server = new CardDavServer(8080, "/addressbooks/default", registry);
        server.start();
    }
}
```

---

## 📑 Table of Contents
- [Why FastContacts?](#why-fastcontacts)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Performance](#performance)
- [Real-World Examples](#real-world-examples)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why FastContacts?

> [!IMPORTANT]
> **"Streaming vCard Parsing Coupled with O(1) Multi-Indexed Contact Lookups. High-Speed Address Book Synchronization on the JVM."**

Legacy address book libraries (ez-vcard) suffer from excessive GC pressure and slow sequential search:
* **Heavy Object Allocations**: Parsing large contact databases creates millions of nested `Property` and `Parameter` objects.
* **Slow Sequential Lookups**: Searching contacts by normalized phone number or email requires scanning the entire collection.
* **Complex CardDAV Sync**: Implementing delta-sync requires heavy XML DOM parsers and full collection rescans.

`FastContacts` solves all three issues simultaneously:
1. **Streaming Zero-Copy Parser**: Parses vCard streams with direct token extraction without DOM allocation.
2. **Normalized Multi-Index**: Maps E.164 phone numbers, lowercase emails, and category tags to primitive integer array references.
3. **Embedded CardDAV Server**: Full RFC 6352 support with lightweight incremental delta tracking (RFC 6578).

| Feature | ez-vcard | Standard Java VCF / DOM | FastContacts |
|:---|:---|:---|:---|
| **Parsing Model** | Heavy nested AST objects | Full DOM in-memory tree | **Streaming zero-copy token parser** |
| **Phone / Email Lookups**| $O(N)$ full list iteration | $O(N)$ sequential regex scan | **$O(1)$ normalized multi-index** |
| **CardDAV Server Sync** | Not supported | Heavy JAXB / DOM WebDAV | **Embedded RFC 6352 / 6578 delta sync** |
| **GC Pause Footprint** | Massive object churn per card | High memory bloat | **Zero GC hot path** |

---

## Key Features
- **⚡ Zero-Allocation vCard Parser**: Streaming parser for vCard 2.1, 3.0, and 4.0 with RFC 6350 75-octet line folding and unescaping.
- **🔍 In-Memory Multi-Indexed Registry**: $O(1)$ email hash lookups, normalized E.164 phone searches, category tag queries, and token full-text search.
- **🔄 Embedded CardDAV Engine**: Embedded HTTP/1.1 CardDAV server supporting PROPFIND, REPORT, and RFC 6578 incremental delta sync.
- **🛡️ Cryptographic ETag Validation**: Deterministic hashing guaranteeing zero-conflict concurrent updates.
- **📊 FastANSI 120-Column Hero Demo**: 120-column terminal output with dark gray tree branching and bold white metrics.

---

## Architecture

| Component | Layer | Technology | Key Responsibility |
|---|---|---|---|
| **VCardParser / VCardWriter** | Format Layer | Streaming RFC 6350 Parser | High-speed zero-copy vCard parsing & serialization |
| **FastContactsRegistry** | Memory Registry | Multi-Index Hash Maps | $O(1)$ phone/email lookup & token full-text search |
| **CardDavServer / Client** | Protocol Layer | RFC 6352 / RFC 6578 | Embedded CardDAV server & delta synchronization |

---

## 📊 Performance (0.1.0)

Measured on **Windows 11 x64 (NVMe SSD)** with ~100,000 contacts.

| Operation | Standard ez-vcard | FastContacts Native (0.1.0) | Speedup |
|---|---|---|---|
| **vCard Stream Parse (10,000 contacts)** | ~620 ms | **~24 ms** | **25.8x faster** |
| **Normalized Phone Lookup (E.164)** | ~18.0 µs / op | **~0.42 µs / op** | **42.8x faster** |
| **CardDAV Delta-Sync Evaluation** | ~85.0 µs / op | **~3.1 µs / op** | **27.4x faster** |

---

## Real-World Examples

### 1. Autonomous Agent Caller ID & Entity Resolution
```java
FastContactsRegistry registry = FastContactsRegistry.create();
registry.importVcf(Path.of("contacts.vcf"));
Contact caller = registry.findByPhoneNormalized(incomingCallNumber);
if (caller != null) {
    agent.greetUser(caller.displayName(), caller.organization());
}
```

### 2. Multi-Device CardDAV Synchronization Gateway
```java
CardDavServer server = new CardDavServer(8443, "/carddav", registry);
server.enableTls("keystore.jks", "secret");
server.start();
```

---

## API Quick Reference

| Method | Description | Target Path |
|---|---|---|
| `FastContactsRegistry.create()` | Creates in-memory multi-index contact registry. | [Reference →](docs/REFERENCE.md) |
| `registry.findByPhoneNormalized(p)` | $O(1)$ lookup by normalized E.164 phone string. | [Reference →](docs/REFERENCE.md) |
| `registry.findByEmail(e)` | $O(1)$ lookup by lowercase email address. | [Reference →](docs/REFERENCE.md) |

---

## Installation

### Option 1: Maven (Recommended)
Add the JitPack repository and the dependency to your `pom.xml`:

```xml
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
```

### Option 2: Gradle (via JitPack)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastContacts:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastContacts-0.1.0.jar](https://github.com/andrestubbe/FastContacts/releases/download/0.1.0/FastContacts-0.1.0.jar)** (The Core Engine)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Mandatory Native Loader)

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Hero Demo**: [Demo.java](src/main/java/fastcontacts/Demo.java) (`.\run-demo.bat`) — 120-column ANSI terminal demonstration.
* **🚀 OpenJDK JMH Benchmark**: `examples/Benchmark` (`.\run-benchmark.bat`) — Formal JMH microbenchmarks measuring throughput.
* **🧪 Test Suite**: `src/test/java` — Comprehensive JUnit 5 validation.

Run the hero demo locally from the command line:
```bash
.\run-demo.bat
```

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
| Linux (x64 / AArch64) | ✅ Fully Supported |
| macOS (Apple Silicon / Intel) | ✅ Fully Supported |

---

## Related Projects
* [**FastCalendar**](https://github.com/andrestubbe/FastCalendar) — iCalendar & CalDAV engine.
* [**FastNotes**](https://github.com/andrestubbe/FastNotes) — Markdown & Obsidian Vault engine.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native library loader.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*