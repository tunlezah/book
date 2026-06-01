# Building Dogear (Deliverable 12 — in progress)

## Requirements
- JDK 17
- Android SDK with platform 35 (compileSdk/targetSdk 35), minSdk 26
- Gradle 8.9 (or let the wrapper handle it)

## First-time setup
The Gradle **wrapper JAR is a binary and is not committed** to the repo. Generate it once
with a locally installed Gradle 8.9:

```bash
gradle wrapper --gradle-version 8.9
```

After that, use the wrapper as usual:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run JVM/Robolectric unit tests
./gradlew lintDebug            # Android Lint
./gradlew installDebug         # install on a connected device/emulator
```

CI (`.github/workflows/ci.yml`) performs these same steps on every push/PR and regenerates
the wrapper automatically.

## Module map
```
:app                 Application, MainActivity, navigation, theming host
:core:model          Pure-Kotlin domain models (no Android deps)
:core:common         Dispatchers, DI qualifiers, hashing
:core:database       Room entities, DAOs, FTS, query builder, mappers, DI
:core:datastore      Settings (Jetpack DataStore)
:core:ui             Design system: theme, palettes, typography, components
:core:cover          Persistent cover/thumbnail cache + generated-cover fallback
:core:ingest         BookImporter (file/URI/folder/ZIP/stream) — shared by library + upload
:feature:library     Shelf (grid/list, sort/filter/search), detail, reader entry, import menu
:feature:reader      Reader engines, controls, themes, position persistence
:feature:upload      Local-network web upload server (NanoHTTPD) + QR
:format:api          BookFormatHandler + FormatRegistry + SafeZip/Magic
:format:epub         EPUB 2/3 metadata + cover (OPF/OCF)
:format:pdf          PDF cover via platform PdfRenderer
:format:comic        CBZ cover (first image, natural order)
:format:text         TXT (catch-all, content-sniffed)
```

## Adding a book (Phase 2)
The shelf top bar has an **Import (+)** action that opens the system file picker; the chosen
file is copied into app storage, hashed, probed, and indexed (metadata + cover + thumbnail)
entirely off the main thread. Duplicate files (same content hash) are skipped. Full
folder/ZIP/web-upload import arrives in Phase 5.

## Notes for the current phase
- **Fonts:** the bundled OFL/free TTFs are not yet added under `core:ui/src/main/res/font`
  (binary assets). `FontRegistry` resolves each choice to its closest generic family until
  they are dropped in; wiring the real fonts is a localized change.
- **Seeding:** with no importer yet, the empty shelf offers **"Add sample books"** to
  populate synthetic books so the grid/list, sort, filter, and search can be exercised.
- Schemas are exported to `core/database/schemas/` on build for migration diffing/testing.
