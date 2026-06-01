# Deliverable 9 — Testing Strategy

Targets high coverage on the logic that matters (parsers, repositories, locator/progress,
security limits) and broad confidence on the UI and end-to-end flows. The architecture
(interfaces + DI + injected dispatchers) is built to be testable.

---

## 1. Test Pyramid

```
        ▲  UI / E2E (Compose + Espresso, fewer, critical journeys)
       ███  Integration (Room, repositories, format handlers on real files)
      █████ Unit (parsers, locators, view models, use cases, security limits) ← most coverage
```

---

## 2. Unit Tests (JVM, fast)
- **Format parsers:** OPF/NCX/nav parsing, spine ordering, metadata extraction, charset
  detection (TXT), FB2/RTF/DOCX transforms, MOBI/PalmDOC decompression — against a fixtures
  corpus of real and hand-crafted files.
- **Locator system:** serialize/deserialize, progression math, re-anchor-by-snippet logic
  (re-import stability), TOC mapping.
- **ViewModels/UseCases:** state transitions with fake repositories and a `TestDispatcher`;
  sort/filter/search logic; reading-state derivation.
- **Cover generation:** deterministic template selection from title hash.
- **Security limits (critical):** zip-bomb caps, ratio/entry/size limits, zip-slip path
  rejection, XML entity expansion disabled — assert safe rejection.
- Tools: JUnit, Truth/AssertK, Turbine (Flow), MockK/fakes, Robolectric where Android types
  are unavoidable.

---

## 3. Integration Tests (instrumented / Robolectric)
- **Room:** DAO queries (sort/filter/search), FTS results, transactions, foreign-key
  cascade, and **migration tests** (`MigrationTestHelper`, every version bump) with
  `exportSchema` JSONs.
- **Repositories:** DB + `FileSource` + `CoverCache` together; duplicate detection by hash;
  progress upsert durability.
- **Format handlers end-to-end:** real EPUB/PDF/CBZ/TXT files → metadata, cover, content
  open, page/spine access.
- **DataStore:** settings round-trips and live updates.

---

## 4. Import Tests
- File picker (single/multi), folder import, ZIP import (mixed supported/unsupported +
  malicious archives), duplicate detection, validation-by-content, error handling. Assert the
  full pipeline (validate → metadata → cover → thumbnails → DB → shelf refresh).

---

## 5. UI Tests (Compose)
- Compose UI tests for shelf (grid/list, sort/filter, search, states), reader controls
  (tap-zones via semantics, theme/font/size changes apply, brightness, keep-awake), TOC
  navigation, bookmarks/highlights/notes flows, settings, and backup/restore.
- Accessibility assertions: TalkBack semantics/content descriptions, focus order, keyboard
  navigation (arrows/PgUp/PgDn/Home/End/Space), large touch targets, contrast on themes.
- First-open tutorial overlay appears/auto-dismisses and respects the setting.

---

## 6. Web Upload Tests
- Server start/stop, port selection, password auth (positive/negative, constant-time),
  **local-only binding**, concurrent uploads, progress, duplicate detection, oversized/zip-bomb
  rejection, traversal-filename rejection, content-validation. Driven by an in-test HTTP
  client against the embedded server.

---

## 7. Performance Tests
- **Macrobenchmark:** startup at seeded 1k/5k libraries (`StartupTimingMetric`), scroll jank
  (`FrameTimingMetric`).
- **Microbenchmark:** parse/metadata/thumbnail/search timings on fixed corpora.
- Memory: profiling checks + LeakCanary gating in debug; assert no leak on reader open/close
  cycles. Budgets enforced (see [08-performance-strategy.md](08-performance-strategy.md) §11).

---

## 8. Malicious-Input Corpus (shared with Security)
A committed, clearly-labeled set of safe-to-store hostile fixtures: zip bombs (capped tiny
representatives + generators), zip-slip archives, billion-laughs XML, truncated/corrupt
EPUB/PDF/MOBI, JS-laden EPUB, oversized-upload simulators, traversal filenames. Every item
has a test asserting **safe rejection** (no crash, no escape, bounded resources).

---

## 9. Coverage & Quality Gates (CI)
- Coverage via JaCoCo/Kover; **targets:** ≥ 85% on `:format:*`, `:core:database`,
  repositories, locator, and security code; ≥ 60% overall (UI dilutes line coverage —
  prioritize critical logic).
- Static analysis: ktlint/detekt, Android Lint, dependency vulnerability scan.
- CI runs unit + Robolectric + migration tests on every PR; instrumented UI + benchmarks on
  a nightly/emulator matrix; PR fails on coverage drop, lint errors, or perf-budget regression.

---

## 10. Test Data Strategy
- A versioned fixtures module with: small valid books per format, edge cases (no cover, huge
  TOC, mixed languages/CJK/RTL, FXL EPUB), and the malicious corpus.
- Seeded-library generators for benchmark runs (1k/5k synthetic books with cached covers).
