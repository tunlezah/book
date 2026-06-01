# Deliverable 5 — Architecture Document

Stack: **Kotlin · Jetpack Compose · MVVM · Repository pattern · Room · Coroutines · Flow ·
Dependency Injection (Hilt).** Offline-first, modular, designed for thousands of books on
2–4 GB devices, and architected for future expansion without redesign.

---

## 1. Guiding Principles

1. **Offline-first.** All core features work with no network. Network features (web upload
   today; cloud/OPDS later) are additive modules behind interfaces.
2. **No work on the UI thread.** Parsing, indexing, cover/thumbnail generation, and search
   run on `Dispatchers.Default/IO`; UI observes `Flow`/`StateFlow`.
3. **Unidirectional data flow.** ViewModel exposes immutable UI state; UI sends events up.
4. **Single source of truth = the database.** Repositories mediate DB + file system; UI
   never touches files or DAOs directly.
5. **Extension points over assumptions.** Format handling, render engines, and sync are
   interfaces with registries, so new formats/backends plug in.

---

## 2. Module Structure (Gradle multi-module)

```
:app                      // Compose Activity, navigation graph, DI wiring, theming host
:core:ui                  // Design system: theme, palettes, typography, shared composables
:core:common              // Result types, dispatchers, coroutine utils, file utils, hashing
:core:database            // Room DB, entities, DAOs, FTS, migrations, type converters
:core:datastore           // Preferences/settings (Jetpack DataStore)
:core:model               // Pure Kotlin domain models (Book, Locator, Highlight, ...)
:feature:library          // Shelf: grid/list, sort/filter/search, detail (ViewModels+UI)
:feature:reader           // Reader screen, render engines, controls, locator/progress
:feature:import           // SAF import, ZIP import, validation, duplicate detection
:feature:upload           // Local web-upload server + UI
:feature:settings         // Settings, themes/palettes, backup/restore, cache mgmt
:feature:annotations      // Bookmarks, highlights, notes managers
:format:api               // BookFormatHandler interface + registry + shared parse models
:format:epub              // EPUB2/EPUB3 parser + reflow source
:format:pdf               // PdfRenderer-based handler
:format:comic             // CBZ (+ gated CBR) image handler
:format:text              // TXT/HTML/FB2/MOBI/AZW3/DOCX/RTF handlers (added incrementally)
:sync:api                 // (future) sync/OPDS/Calibre interfaces — defined, not implemented
```

Module boundaries enforce the layering and keep build times and APK modular. Features depend
on `:core:*`, `:core:model`, and `:format:api` — never on each other.

---

## 3. Layered Architecture (per feature)

```
 Compose UI  ──events──▶  ViewModel  ──▶  UseCase/Repository  ──▶  DAO (Room) / FileSource
     ▲                        │                                         │
     └────── StateFlow<UiState> ◀── Flow ◀───────────────────────────────┘
```

- **UI (Compose):** stateless composables driven by `UiState`; emit events. No business
  logic; previewable.
- **ViewModel:** holds `StateFlow<UiState>`, runs use cases in `viewModelScope`, survives
  config changes; reading position also persisted to DB so it survives process death.
- **Repository:** the single source of truth API; combines DAO `Flow`s with file operations;
  exposes domain models from `:core:model`.
- **Data sources:** Room DAOs (DB), `FileSource`/SAF (storage), `CoverCache` (disk+memory),
  `PreferencesSource` (DataStore).

---

## 4. Format Handling Extension Point

```kotlin
interface BookFormatHandler {
    val supportedFormats: Set<BookFormat>
    suspend fun probe(source: FileRef): Boolean              // sniff magic bytes / structure
    suspend fun extractMetadata(source: FileRef): BookMetadata
    suspend fun extractCover(source: FileRef): RawImage?
    fun openContent(source: FileRef): BookContent            // reflow | fixedPage | imagePager
}

sealed interface BookContent {
    interface Reflowable : BookContent {                     // EPUB, HTML, FB2, TXT, MOBI...
        suspend fun spine(): List<SpineItem>
        suspend fun resource(href: String): Resource         // streamed, lazy
        suspend fun toc(): List<TocEntry>
    }
    interface FixedPage : BookContent {                      // PDF
        val pageCount: Int
        suspend fun renderPage(index: Int, target: Size): Bitmap
    }
    interface ImagePager : BookContent {                     // CBZ/CBR
        val pageCount: Int
        suspend fun page(index: Int, target: Size): Bitmap
    }
}
```

A `FormatRegistry` (DI-provided) maps file → handler via `probe()`. Adding a Tier 2/3 format
means dropping in a new handler module — no changes to library/reader cores.

### Render engines (consume `BookContent`)
- **ReflowEngine** — a `WebView` per reader, paginated with CSS multi-column; injects reading
  CSS (font/spacing/theme), serves resources via `WebViewAssetLoader` straight from the
  archive (no full extraction). JavaScript bridge only for our pagination/locator logic;
  publisher scripts disabled (security).
- **FixedPageEngine** — PDF page raster with tiling/zoom and bitmap recycling.
- **ImagePagerEngine** — comic pager with neighbor prefetch and downscaling.

---

## 5. Locator System (position persistence)

A robust, format-agnostic `Locator` makes "reopen exactly where you stopped" and
re-import-stable highlights possible:

```kotlin
data class Locator(
    val spineIndex: Int? = null,      // reflowable: index into spine
    val cssSelectorOrXPath: String? = null,
    val charOffset: Int? = null,      // within element/text node
    val textSnippet: String? = null,  // anchor for re-find after re-import
    val pageIndex: Int? = null,       // fixed/comic
    val progression: Float            // 0f..1f within book (always set)
)
```

- Reflowable: anchor by element selector + char offset + a short text snippet, so a re-import
  (slightly different file) can re-locate by text search; `progression` is the fallback.
- Fixed/comic: page index + scroll fraction.
- Progress writes are **frequent and atomic** (debounced, on pause/turn/stop) to survive
  crashes, power loss, and force-kill.

---

## 6. Concurrency & Background Work

- **Coroutines + Flow** everywhere; structured concurrency scoped to ViewModels/repositories.
- **Dispatchers** injected (testability): `IO` for disk/zip/db, `Default` for parse/decode.
- **Indexer:** a background pipeline (WorkManager for durable jobs + in-process coroutine
  queue for foreground responsiveness) does metadata/cover/text-index extraction off-thread,
  emitting progress to the shelf.
- **Backpressure:** bounded channels/semaphores cap concurrent decodes to protect memory on
  2 GB devices.

---

## 7. Dependency Injection (Hilt)

- `@HiltAndroidApp` app; `@AndroidEntryPoint` activity. Modules provide DB, DAOs, DataStore,
  dispatchers, `FormatRegistry`, caches, repositories. Interfaces enable fakes for tests.

---

## 8. State, Settings, and Theming

- **Settings:** Jetpack DataStore (typed, `Flow`-based). Theme/palette/font/reader prefs are
  observed so changes apply live across shelf/reader/settings.
- **Theming:** `:core:ui` exposes `EarmarkTheme(palette, mode)`; reading theme is separate
  state consumed only by the reader. Palettes are static `ColorScheme`s + optional dynamic
  color on Android 12+.

---

## 9. Future Expansion (designed, not built)

`:sync:api` defines interfaces so future modules implement them without core changes:

```kotlin
interface SyncBackend { suspend fun pull(...); suspend fun push(...) }   // cloud / multi-device
interface CatalogSource { suspend fun browse(url): List<CatalogEntry> }  // OPDS
interface RemoteFileSource { /* SMB / NAS / Calibre content server */ }
```

- The `Locator` and annotation models are already serialization-friendly for sync.
- The repository layer can gain remote sources behind the same interfaces.
- **None are implemented in v1** — only the seams exist.

---

## 10. Key Decisions Log

| Decision | Choice | Why |
|----------|--------|-----|
| EPUB rendering | WebView + CSS multi-column reflow | Highest fidelity, lowest effort/memory vs. native engine |
| PDF | Platform `PdfRenderer` | No AGPL, small APK; Pdfium later if needed |
| DI | Hilt | First-class Android/Compose support, compile-time safety |
| Persistence | Room + FTS + DataStore | Fast indexed library/in-book search; typed prefs |
| Async | Coroutines/Flow + WorkManager | Off-thread everything; durable indexing |
| Modularization | Multi-module Gradle | Build speed, enforced boundaries, format plug-ins |
| Image cache | Coil + custom disk thumbnail cache | Memory-aware, off-thread, content-hash keyed |
