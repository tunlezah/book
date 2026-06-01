# Deliverable 8 — Performance Strategy

Performance is a P1 product goal. The app must feel instant on low-end tablets (2/3/4 GB
RAM). This document sets the targets, the per-area budgets, and the concrete techniques,
plus how each is measured.

---

## 1. Targets (from the brief)

| Area | Target |
|------|--------|
| Startup (1,000 books) | **< 2 s** to interactive shelf |
| Startup (5,000 books) | **< 3 s** |
| Shelf scrolling | No visible stutter (≈ 60 fps; no frame > 16 ms sustained) |
| Cover loading | No visible lag (placeholder instant, async fill) |
| Reading transitions | Instant (page turn < 1 frame perceptible) |
| Memory leaks | **Zero** known leaks |

---

## 2. Memory Budget (low-end first)
On a 2 GB device the per-app heap may be ~96–192 MB. Working budget:
- **Shelf:** virtualized; only visible covers + a small prefetch window held as bitmaps.
- **Bitmap memory cache:** capped LRU sized to a fraction of `ActivityManager.memoryClass`
  (e.g., 1/8), `RGB_565` for thumbnails where acceptable, downscaled to display size.
- **Reader (reflow):** one spine item (+ neighbors) in the WebView; no whole-book inflation.
- **Reader (PDF/comic):** 1–2 page bitmaps at viewport resolution, recycled.
- **Parsers:** streaming; bounded buffers; no whole-archive extraction.
- Respond to `onTrimMemory`/`ComponentCallbacks2` by shrinking caches.

---

## 3. Startup Strategy (the < 2 s promise)
The shelf must be interactive fast even with thousands of rows:
1. **Don't load the world.** Library list is **paged (Paging 3)** straight from Room with
   indexed sort — only the first screen's rows are queried; covers come from disk cache.
2. **Covers are pre-cached.** Thumbnails are generated at import/index time, so startup only
   reads small files from disk, never parses books.
3. **Defer indexing.** New/changed files are indexed *after* the shelf is shown, via the
   background indexer, with progress shown non-blockingly.
4. **Baseline Profiles + R8.** Ship a Baseline Profile for the startup + scroll paths;
   enable R8 full mode/resource shrinking to cut class-load and code size.
5. **Lazy DI/initialization.** Avoid heavy work in `Application.onCreate`; use App Startup /
   lazy providers; no synchronous disk/db on the main thread.
6. **Minimal first frame.** Compose shelf renders placeholders immediately; data streams in.

Result: cold-start cost is dominated by process init + first DB page + first-screen
thumbnails — independent of total library size (1k vs 5k differs mainly in index catch-up,
which is backgrounded).

---

## 4. Scrolling Strategy (no stutter)
- `LazyVerticalGrid`/`LazyColumn` with **stable keys** and lightweight item composables.
- **Coil** async image loading with memory+disk cache, correct target sizing, and
  placeholders; `RGB_565` thumbnails; no main-thread decode.
- Avoid recomposition churn: hoist state, use `derivedStateOf`, immutable item models,
  `@Stable`/`@Immutable` annotations.
- Prefetch a small lookahead window; cancel loads for items scrolled past.
- Verified with Macrobenchmark `FrameTimingMetric` (jank %, P50/P90/P99 frame time).

---

## 5. Cover/Thumbnail Pipeline
- Generated **once** at index/import, keyed by `content_hash`; persisted to disk; never
  regenerated unless missing.
- Two thumbnail sizes (grid, list) at device density; decoded with `inSampleSize`/`Coil`
  downsampling — never decode full-res cover art into the shelf.
- Generation runs on the indexer (bounded concurrency) — off the UI thread, with backpressure.

---

## 6. Reading Transition Strategy (instant)
- Default animation **None** → page turn is a column-offset translate in the WebView (no
  re-layout): pre-paginate the current spine item, move the viewport by one column width.
- Pre-render adjacent pages/columns so a turn is a cheap transform, not a parse.
- PDF/comic: pre-decode neighbor pages during idle.
- Heavier animations (slide/fade/curl) are opt-in and skipped on low-end if `isLowRamDevice`.

---

## 7. Parsing & Indexing
- **Incremental/streaming** parsing: read OPF/spine lazily; load resources on demand from
  the archive; never inflate the whole book.
- Indexer pipeline: probe → metadata → cover/thumbnail → (lazy) text index, each stage
  cancellable and bounded; durable via WorkManager so it resumes after kill.
- In-book text index built lazily and cacheable/clearable.

---

## 8. No-Main-Thread-Work Rule
- StrictMode (debug) flags disk/network on main thread and leaked closeables.
- All I/O on `Dispatchers.IO`, CPU work on `Dispatchers.Default`; UI only observes `Flow`.

---

## 9. Leak Prevention (zero leaks)
- **LeakCanary** in debug for every screen, especially the reader WebView (a classic leak
  source): destroy/await the WebView properly, avoid Activity context capture, null out
  bridges on teardown.
- Lifecycle-aware collection (`repeatOnLifecycle`); cancel scopes; close cursors/streams in
  `use {}`.
- Bitmap recycling for PDF/comic; rely on Coil for shelf bitmaps.

---

## 10. Measurement & Tooling (how we prove the targets)
| Metric | Tool |
|--------|------|
| Startup (cold/warm) at 1k & 5k books | Jetpack **Macrobenchmark** `StartupTimingMetric` on seeded libraries |
| Scroll jank | Macrobenchmark `FrameTimingMetric` |
| Import / thumbnail / search speed | Microbenchmark + instrumented timers on fixed corpora |
| Memory usage / leaks | Android Studio profiler, `Debug.MemoryInfo`, LeakCanary |
| Code/APK size, font budget | APK Analyzer; tracked budgets in CI |

Benchmarks run on representative low-end profiles (emulated 2 GB) and a real 8-inch tablet
where available, with results recorded in the benchmark report (Deliverable 14).

---

## 11. Performance Budgets (CI-tracked)
- Cold start 1k ≤ 2 s, 5k ≤ 3 s (regression fails CI if exceeded by > 10%).
- Scroll: < 1% janky frames on the seeded 1k grid.
- APK size budget (incl. fonts) tracked; font set ≤ ~10 MB.
- Reader memory ≤ budget on 2 GB profile for a large EPUB.
