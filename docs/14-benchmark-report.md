# Deliverable 14 — Benchmark Report

This report defines the benchmark methodology, the budgets being enforced, and where results
are recorded. Numbers below are **targets/placeholders**: they are filled in from runs on real
hardware and the emulator matrix (the build environment used for development has no Android
device, so live figures are produced in CI / on-device).

---

## 1. Methodology

| Metric | Tool | Setup |
|--------|------|-------|
| Cold/warm startup | Jetpack **Macrobenchmark** `StartupTimingMetric` | Seeded libraries of 1,000 and 5,000 books (covers pre-cached) |
| Scroll jank | Macrobenchmark `FrameTimingMetric` | Fling the 1,000-book grid; report % janky frames, P50/P90/P99 frame time |
| Import / thumbnail / search | Instrumented timers + Microbenchmark | Fixed corpora (EPUB/PDF/TXT/CBZ) |
| Memory / leaks | Android Studio profiler, `Debug.MemoryInfo`, **LeakCanary** | Reader open/close cycles on a large EPUB |
| APK size + font budget | APK Analyzer | Per-build, tracked against budgets |

Seeding for startup/scroll runs uses the in-app **"Add sample books"** generator (synthetic
metadata with cached covers), so startup cost is independent of total library size.

Run locally (device/emulator attached):
```bash
./gradlew :app:connectedReleaseAndroidTest          # instrumented + macrobenchmark
```

---

## 2. Budgets (CI-enforced, Performance Strategy §11)

| Budget | Target | Fail threshold |
|--------|--------|----------------|
| Cold start, 1,000 books | < 2.0 s | > 2.2 s |
| Cold start, 5,000 books | < 3.0 s | > 3.3 s |
| Scroll jank (1k grid) | < 1% janky frames | ≥ 2% |
| Reader open → first page | < 1.0 s (Tier-1 formats) | > 1.5 s |
| Reader memory (large EPUB, 2 GB profile) | within heap budget | OOM / trim storms |
| Known leaks | 0 | any |
| Bundled font footprint | ≤ ~10 MB | > 12 MB |

---

## 3. Results (to be populated on hardware)

| Run | Device / profile | Cold 1k | Cold 5k | Scroll jank | Reader open | Notes |
|-----|------------------|---------|---------|-------------|-------------|-------|
| — | Pixel-class emulator (API 34) | _TBD_ | _TBD_ | _TBD_ | _TBD_ | |
| — | 2 GB low-RAM emulator | _TBD_ | _TBD_ | _TBD_ | _TBD_ | |
| — | 8-inch tablet (Android 13) | _TBD_ | _TBD_ | _TBD_ | _TBD_ | primary target |

---

## 4. Design choices that back the budgets

- **Startup independent of library size**: Paging 3 queries only the first screen from Room;
  covers are pre-generated and read as small files; indexing is deferred and backgrounded.
- **No main-thread work**: injected dispatchers; StrictMode in debug flags violations.
- **Memory-aware decode**: thumbnails downsampled to RGB_565; reader loads one spine item /
  one page at a time; PDF/comic bitmaps at view resolution and recycled.
- **Leak prevention**: WebView destroyed + detached + bridge nulled on dispose; LeakCanary in
  debug gates regressions.
- **Server resource bounds**: upload connections capped by a fixed thread pool.

When the first on-device runs land, this table is updated and any budget miss is filed as a
release blocker.
