# FastContacts Roadmap

This document outlines the strategic engineering roadmap for **FastContacts** within the FastJava ecosystem.

---

## 🏁 Phase 1: Core Engine & vCard Standards (v0.1.0 — Current)
- [x] Streaming low-allocation parser for vCard 2.1, 3.0, and 4.0.
- [x] RFC 6350 / RFC 2426 serializer with 75-column line folding.
- [x] High-concurrency in-memory registry with multi-index search (Email, Phone E.164, Category, Inverted Prefix Tokens).
- [x] Embedded RFC 6352 CardDAV server with RFC 6578 `sync-collection` delta tracking.
- [x] FastANSI 120-column Hero HUD.
- [x] JMH Benchmark suite and complete documentation.

---

## 🚀 Phase 2: Vector Search & SIMD Optimizations (v0.2.0)
- [ ] **AVX2 / Vector API Substring Scanning**: Vectorized token matching over raw byte buffers using Java Vector API / SIMD intrinsics.
- [ ] **jCard (RFC 7095) & xCard (RFC 6351)**: Direct zero-copy JSON and XML vCard representations for LLM tool agents and browser frontends.
- [ ] **FastAIMemory & FastAI Embeddings Linking**: Semantic vector indexing of contact notes, biographies, and interaction histories.

---

## ⚡ Phase 3: Persistent Storage & Enterprise Sync (v0.3.0)
- [ ] **FastFileIndex / LMDB Persistence**: Memory-mapped zero-copy disk persistence for multi-million contact enterprise registries.
- [ ] **Bi-Directional OAuth2 CardDAV Sync**: Native connectors for Google Contacts and Microsoft Graph API.
- [ ] **Call & Message Dispatcher**: Direct IPC integration with FastAudio / FastNotification modules for agentic telephony workflows.
