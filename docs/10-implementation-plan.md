# Deliverable 10 — Implementation Plan

A phased, milestone-based plan that begins **only after the research/design docs above are
approved**. Sequencing follows the product priorities: P1 (speed, EPUB, memory, covers, UI)
first, then P2 (upload, navigation, persistence, brightness/awake), then P3 (advanced
library, typography depth, annotations). Each milestone is independently testable and leaves
the app in a working state.

---

## Phase 0 — Project Scaffold & Foundations
- Gradle multi-module skeleton (`:app`, `:core:*`, `:feature:*`, `:format:*`, `:sync:api`)
  with version catalog, R8, Compose, Hilt, Room, DataStore, Coil, WorkManager wired.
- `:core:ui` design system: `DogearTheme`, the 10+ palettes, typography/`FontRegistry`,
  bundled fonts, shared composables.
- CI: build + ktlint/detekt + unit tests + coverage; baseline-profile + benchmark scaffolding.
- **Exit:** app launches to an empty themed shelf; CI green.

## Phase 1 — Library Core & Persistence (P1)
- `:core:database` Room schema (Deliverable 6) with FTS, indices, exported schemas,
  migration harness.
- `:feature:library`: paged shelf (grid/list), sort, filter, FTS search, reading states,
  detail screen — all on seeded data.
- **Exit:** 5k seeded books scroll smoothly; startup-benchmark harness reports cold-start.

## Phase 2 — Format Engine & Covers (P1)
- `:format:api` + `FormatRegistry`; Tier-1 handlers: **EPUB2/3, TXT, PDF, CBZ**.
- Background **indexer**: probe → metadata → cover extraction → thumbnail cache (content-hash
  keyed, persistent) → DB; template-cover generation when none exists.
- **Exit:** import a real EPUB/PDF/TXT/CBZ; covers cached; metadata populated; meets cover/
  scroll perf budgets.

## Phase 3 — Reader: Reflow + Fixed + Comic (P1, the centerpiece)
- ReflowEngine (WebView + CSS multi-column) with injected reading CSS, asset-loader resource
  serving, **JS disabled** per security; FixedPageEngine (PDF); ImagePagerEngine (CBZ).
- Distraction-free reading, **tap zones**, auto-hiding controls, instant page turns,
  first-open tutorial overlay.
- **Locator system** + atomic **reading-position persistence** (survives crash/force-kill);
  TOC / go-to-page / go-to-percentage.
- **Exit:** open each Tier-1 format, reopen exactly where stopped, transitions instant,
  reader passes leak checks.

## Phase 4 — Reading Controls, Themes, Screen Mgmt (P1/P2)
- Reading controls: font, size, weight, line/paragraph spacing, margins, alignment,
  hyphenation, page animation.
- Reading themes (8 built-in + custom) and the app palettes applied everywhere.
- Brightness (reader/system/auto), keep-awake (never/reading/charging), orientation lock,
  fullscreen.
- **Exit:** all controls live-apply across reader; themes affect all surfaces.

## Phase 5 — Import & Web Upload (P2)
- `:feature:import`: SAF single/multi/folder, ZIP import, duplicate detection, validation —
  with all archive security limits.
- `:feature:upload`: local-only embedded server (enable/disable, port, optional password,
  QR), full upload pipeline, concurrency, progress, dedupe, and **all upload-server security
  controls** (Deliverable 7).
- **Exit:** upload from a browser on the LAN → book appears automatically; security tests pass.

## Phase 6 — Annotations & Search Depth (P3)
- `:feature:annotations`: bookmarks (named, searchable, manager), highlights (colors,
  manager, re-import-stable anchoring), notes (on-highlight + standalone, export).
- In-book search (lazy text index, FTS); library search polish.
- **Exit:** annotate, search in-book, export notes; annotations survive re-import where possible.

## Phase 7 — Metadata, Settings, Backup/Restore (P2/P3)
- Metadata edit / cover replace / refresh (respecting `metadata_locked`).
- Centralized settings (all required items); cache management (sizes + clear).
- Backup/restore of DB + progress + annotations + collections + settings to a portable,
  atomically-restored archive.
- **Exit:** backup on one device restores full state on another.

## Phase 8 — Accessibility, Foldables, Input (cross-cutting)
- TalkBack semantics throughout, keyboard navigation (arrows/PgUp/PgDn/Home/End/Space),
  volume-key paging (optional), high-contrast theme, dynamic text sizing.
- Window-size-class adaptive layouts (compact/medium/expanded), foldable posture handling,
  RTL.
- **Exit:** accessibility + adaptive UI tests pass on phone/8-inch/foldable/landscape.

## Phase 9 — Hardening, Benchmarks, Docs
- Full security test corpus green; LeakCanary clean; perf budgets met (1k < 2 s, 5k < 3 s,
  no scroll jank).
- Benchmark report (Deliverable 14), build instructions (12), user documentation (15).
- **Exit:** all deliverables 11–15 complete; release-candidate build.

## Phase 10 — Tier-2/3 Formats (post-core, additive)
- Add FB2, HTML, MOBI, AZW3 (Tier 2) then DOCX, RTF, and (license-gated) CBR (Tier 3) as new
  handler modules behind `BookFormatHandler` — no core redesign.

---

## Sequencing Rationale
1. **Persistence + shelf first** so performance is provable early and everything has a home.
2. **Format engine + covers** before the reader, since the reader consumes `BookContent` and
   covers drive the shelf experience.
3. **Reader** is the largest, most differentiating piece — built once foundations are solid.
4. **Upload/import** and **annotations** layer on without touching the reading core.
5. **Hardening/benchmarks** continuously, formalized at the end.

## Cross-Cutting Throughout
- No main-thread I/O; injected dispatchers; tests written alongside each milestone (TDD for
  parsers/security).
- Security limits enforced from the first archive/parser code, not retrofitted.
- Performance budgets checked in CI from Phase 1.
- Future-expansion seams (`:sync:api`, serializable locators/annotations) kept intact.

---

## Post-Approval Deliverables (11–15) Mapping
- **11 Source code** — produced across Phases 0–10.
- **12 Build instructions** — finalized Phase 9 (added incrementally from Phase 0).
- **13 Test suite** — grown every phase, gated in CI.
- **14 Benchmark report** — Phase 9 (harness from Phase 1).
- **15 User documentation** — Phase 9.
