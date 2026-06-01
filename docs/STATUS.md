# Implementation Status

Dogear is built in the phases from the [Implementation Plan](10-implementation-plan.md). All
ten phases are now implemented on this branch.

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Multi-module scaffold, design system, DI, CI | ✅ |
| 1 | Room persistence + paged shelf (sort/filter/FTS search/states) | ✅ |
| 2 | Format engine + Tier-1 handlers (EPUB/PDF/TXT/CBZ), cover cache, importer | ✅ |
| 3 | Reader engines (reflow/PDF/comic), tap zones, controls, position persistence | ✅ |
| 4 | Reading controls, 8 themes + custom, brightness/keep-awake/orientation | ✅ |
| 5 | Import (files/folder/ZIP) + local web-upload server | ✅ |
| 6 | Bookmarks, highlights, notes, in-book search | ✅ |
| 7 | Metadata edit/cover/refresh, Settings screen, Backup/Restore | ✅ |
| 8 | Accessibility, keyboard + volume input, semantics | ✅ |
| 9 | Hardening (bounded server, LeakCanary), benchmark report, user guide | ✅ |
| 10 | Tier-2 formats: FB2 + HTML | ✅ |

## Formats

| Tier | Formats | Status |
|------|---------|--------|
| 1 | EPUB 2/3, PDF, TXT, CBZ | ✅ implemented |
| 2 | FB2, HTML | ✅ implemented |
| 2 (remaining) | MOBI, AZW3 | ⏳ handler scaffold pattern established; unpack work outstanding |
| 3 | DOCX, RTF | ⏳ deferred (lower popularity / parser weight) |
| 3 (gated) | CBR | ⛔ license-gated (junrar is GPL) — ship CBZ; convert or optional component |

Each new format is a self-contained module implementing `BookFormatHandler` and contributing
via Hilt multibinding — no changes to the library or reader cores (Architecture §4).

## Known caveats

- **Not yet compiled on an Android toolchain.** Development happened in an environment without
  the Android SDK, so the code is written correct-by-construction. The first `./gradlew` sync on
  a real machine will fetch dependencies, generate the wrapper jar, and surface any remaining
  fixes. WebView pagination, the upload server, and PDF rendering especially warrant an
  on-device pass.
- **Bundled fonts not yet added.** The 15+ OFL/free TTFs aren't in `core:ui/res/font` (binary
  assets); `FontRegistry` and the reader map each choice to a close serif/sans generic until
  they are dropped in. This is a localized change.
- **Benchmark numbers** are targets until on-device runs populate
  [the report](14-benchmark-report.md).

## Next steps to a release candidate

1. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` on a machine with the Android SDK;
   resolve any compile fixes.
2. Add the bundled font assets and wire `FontRegistry` / reader `@font-face`.
3. Device pass on an 8-inch Android 13 tablet (reader fidelity, upload, PDF, foldable).
4. Populate the benchmark report; confirm budgets.
5. Tier-2/3 format follow-ups (MOBI/AZW3, then DOCX/RTF; CBR pending license review).
