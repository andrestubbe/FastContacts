# Changelog

All notable changes to **FastContacts** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-08-28

### Added
- **High-Performance vCard Engine**:
  - Streaming zero-allocation line unfolding parser for vCard 2.1, 3.0, and 4.0 (`VCardParser`).
  - RFC 6350 / RFC 2426 compliant serializer with 75-character line folding and character escaping (`VCardWriter`).
  - Structured fields support: Full Names (`FN`, `N`), Nickname, Organization, Job Title, Role, Emails with parameters, Phones with types/PREF, Structured Postal Addresses (`ADR`), Categories/Tags, Notes, Birthday, URLs, Base64 Avatars/Photos, and Custom Properties (`X-*`).
- **Concurrent In-Memory Contacts Registry (`FastContactsRegistry`)**:
  - Sub-microsecond exact hash email lookup (`findByEmail`).
  - E.164 normalized digits phone lookup (`findByPhone`).
  - Category / Tag cohort index (`findByCategory`).
  - Inverted token & prefix search engine (`search` with `ContactSearchQuery`).
  - RFC 6578 `sync-token` delta change-tracking engine with tombstone deletion logs.
  - Streaming bulk `.vcf` import and export.
- **Embedded CardDAV Server (`CardDavServer`)**:
  - Pure Java 17+ HTTP/1.1 CardDAV implementation (`HttpServer` + Virtual Threads).
  - RFC 6352, RFC 4791, and RFC 4918 compliance (`PROPFIND`, `REPORT`, `GET`, `PUT`, `DELETE`, `OPTIONS`).
  - Address book discovery with `/.well-known/carddav` redirect.
  - Native `sync-collection` report handling for incremental delta sync with Apple Contacts, Thunderbird, and Nextcloud.
- **CardDAV Client (`CardDavClient`)**:
  - High-throughput client for syncing contacts, pushing updates, and executing delta reports.
- **FastANSI 120-Column Terminal HUD (`FastAnsiContactsHUD`)**:
  - 120-column terminal output with dark gray tree branches (`├──`, `└──`), bold white values, and middle-path truncation.
- **Hero Demo (`Demo.java`, `run-demo.bat`)**:
  - Ingests 100,000+ contacts in milliseconds.
  - Sub-microsecond query latencies and live CardDAV delta synchronization demo.
- **OpenJDK JMH Benchmark Suite (`examples/Benchmark`, `run-benchmark.bat`)**:
  - Microbenchmarking vCard parsing, serialization, email lookup, phone lookup, and multi-field queries.
