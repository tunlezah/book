# Deliverable 15 — Book-Opening Robustness (research + hardening)

Driven by three research passes into how real readers (Readium, epub.js, Foliate, KOReader,
Thorium, Librera, ReadEra, calibre) fail to open or mis-render books, plus the Android
platform behaviors behind those failures. This doc records the findings, the root-cause bug we
hit, what we applied, and what remains planned. Sources are linked inline.

---

## 0. The bug that started this: EPUB wouldn't open

Symptom on device: `net::ERR_HTTP_RESPONSE_CODE_FAILURE` for a `data:text/html;…;base64,` URL,
cream background, no content.

Root cause: the reflow WebView's `shouldInterceptRequest` returned an **HTTP 403** for any
request whose host wasn't our virtual host — to block network egress. But the WebView's **own
main-frame document** (loaded via `loadDataWithBaseURL`, internally a `data:` URL) is routed
through the interceptor, so it got the 403. Chromium treats a **≥400 status on the main frame**
as a hard navigation failure → `ERR_HTTP_RESPONSE_CODE_FAILURE`, blank page. No reflowable book
could ever render.
([WebResourceResponse status validation](https://android.googlesource.com/platform/frameworks/base/+/a8352f4/core/java/android/webkit/WebResourceResponse.java),
[issuetracker 232805585](https://issuetracker.google.com/issues/232805585))

**Fix (applied):** serve in-book resources through the official
[`WebViewAssetLoader`](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content)
over `https://appassets.androidplatform.net/book/<path>`; it returns `null` for anything it
doesn't own (including the inline main document), so the frame renders. External egress is now
blocked with an **empty 200**, never an error status. This is exactly the approach the **Readium
Kotlin toolkit migrated to** when it deleted its local HTTP server in 3.0
([readium/kotlin-toolkit#34](https://github.com/readium/kotlin-toolkit/issues/34),
[migration guide](https://github.com/readium/kotlin-toolkit/blob/develop/docs/migration-guide.md)).

---

## 1. EPUB-in-WebView findings → applied

| Finding (source) | Status |
|---|---|
| Never return ≥400 for the main frame; block egress with empty 200 ([Android src](https://android.googlesource.com/platform/frameworks/base/+/a8352f4/core/java/android/webkit/WebResourceResponse.java)) | ✅ applied |
| Use `WebViewAssetLoader` + real https origin (Readium did the same) ([#34](https://github.com/readium/kotlin-toolkit/issues/34)) | ✅ applied |
| Preserve internal directory structure so `../images`, `url(../fonts)` resolve | ✅ applied (base URL = spine dir) |
| Image overflow into next column → `img{max-width:100%;max-height;object-fit:contain;box-sizing:border-box;break-inside:avoid}` ([epub.js#786](https://github.com/futurepress/epub.js/issues/786), [readium-css#55](https://github.com/readium/readium-css/issues/55)) | ✅ applied |
| Charset: honor BOM/meta/XML decl, default UTF-8 ([calibre](https://manual.calibre-ebook.com/edit.html)) | ✅ applied (jsoup byte parse) |
| Lenient HTML5 parse for undeclared entities/malformed markup ([calibre-web#1931](https://github.com/janeczku/calibre-web/issues/1931)) | ✅ applied (jsoup) |
| CSP `default-src 'none'; connect-src 'none'` to stop egress at source | ⏳ planned |
| Snap column width / offsets to integer CSS px; repaginate after `load`; debounce resize ([readium/css#97](https://github.com/readium/css/issues/97), [epub.js#1384](https://github.com/futurepress/epub.js/issues/1384)) | ✅ applied (JS sets exact-px `column-width`, settles after `fonts.ready`+stability poll, debounced resize re-anchor) |
| Page by the real column pitch (`clientWidth`/`scrollLeft` of the columned box), **not** `window.innerWidth`/`scrollTo`; the `overflow:hidden` body is the scroller, the window is not ([epub.js delta = width+gap−padding](http://epubjs.org/documentation/0.3/)) | ✅ applied |
| Give the WebView a definite size (`fillMaxSize`) so `100%`/`100vh` resolve to a real viewport (else columns collapse → blank page) | ✅ applied |
| Font de-obfuscation (IDPF 1040-byte / Adobe 1024-byte XOR keyed on OPF id) ([epubsecrets](https://epubsecrets.com/font-embedding-and-font-obfuscationmangling.php)) | ⏳ planned |
| Fixed-layout EPUB3 separate code path (viewport meta, `page-spread-*`, RTL) ([IDPF FXL](https://idpf.org/epub/fxl/)) | ⏳ planned |
| Resolve all `srcset`/`<picture>` candidates; block `javascript:`; MathJax for MathML | ⏳ planned |

## 2. Cross-format opening findings → applied

| Finding (source) | Status |
|---|---|
| Don't gate EPUB opening on mimetype/validity; it's advisory ([epubcheck#69](https://github.com/w3c/epubcheck/issues/69), [booklore#2997](https://github.com/booklore-app/booklore/issues/2997)) | ✅ already (probe accepts container or mimetype) |
| OPF discovery: container.xml → first rootfile → scan zip for any `*.opf` | ✅ applied |
| Skip spine itemrefs missing from the manifest; build order from what resolves ([EpubReader#109](https://github.com/vers-one/EpubReader/issues/109)) | ✅ already (mapNotNull) |
| Namespace-agnostic OPF/DC parsing (match by local-name) | ✅ already |
| Forgiving href resolution: decoded/encoded/case-insensitive/backslash; strip `#frag` ([Apple asset guide](https://help.apple.com/itc/booksassetguide/en.lproj/static.html)) | ✅ applied (entry index) |
| TOC fallback nav → NCX → synthesized-from-spine ([EpubReader#41](https://github.com/vers-one/EpubReader/issues/41)) | ◑ nav→NCX done; synthesized = ⏳ |
| Detect DRM (encryption.xml AES/ADEPT) → clear message, never bypass; distinguish from font obfuscation ([KOReader#8222](https://github.com/koreader/koreader/issues/8222), [DeDRM](https://deepwiki.com/noDRM/DeDRM_tools/6.1-epub-post-processing)) | ✅ applied |
| Per-resource isolation: one bad chapter/image degrades, never aborts the book | ✅ already (runCatching per resource) |
| Huge single spine files → chunk/incremental ([Thorium#1326](https://github.com/edrlab/thorium-reader/issues/1326)) | ⏳ planned |
| ZIP names UTF-8→CP437 fallback; reject path traversal | ◑ traversal N/A (read-by-name); CP437 = ⏳ |

## 3. PDF findings → applied

| Finding (source) | Status |
|---|---|
| Render `ARGB_8888` (required), pre-fill white (avoid blank), Mutex-serialize, one page open at a time ([PdfRenderer docs](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)) | ✅ already |
| Encrypted PDF → `SecurityException`: surface "password-protected", don't crash | ✅ applied (classified) |
| Corrupt/zero-page → `IOException`: surface "corrupt or unreadable" | ✅ applied (classified) |
| Never reuse a renderer after a failure (pre-API-28 state leak, [graphics-samples#41](https://github.com/android/graphics-samples/issues/41)) | ✅ already (fresh per open) |
| Clamp bitmap bytes vs `memoryClass`; prefetch ±1 on 2 GB | ⏳ planned |
| API 35 `LoadParams` password / `PdfRendererPreV` (30–34); adopt `androidx.pdf` when stable | ⏳ planned |

---

## 3b. The second bug: EPUB rendered once, then blank & un-pageable

Symptom on device: the book showed text once, then on reopen the page area was blank and page
turns did nothing (exit still worked). Three compounding causes, all fixed:

1. **WebView had no size.** `AndroidView(modifier = Modifier)` left the WebView unconstrained, so
   CSS `100%`/`100vh` resolved against a wrong/zero viewport and the column box collapsed. → now
   `Modifier.fillMaxSize()`.
2. **Wrong scroll lever.** The columned `body` is `overflow:hidden`, so *it* is the scroll
   container — but the pager drove `window.scrollTo`/`window.scrollX`, which are no-ops on an
   overflow-hidden body. Page turns therefore never moved. → now scroll/measure via
   `body.scrollLeft` / `body.scrollWidth` / `body.clientWidth`.
3. **Wrong pitch + premature measurement.** Paging stepped by `window.innerWidth` (which includes
   the reading padding) instead of the true column pitch, and position was restored in
   `onPageFinished` before web fonts/images reflowed the text — landing on a blank offset. → the
   horizontal reading margin moved to `margin` (zero horizontal padding ⇒ pitch === `clientWidth`),
   `column-gap:0`, exact-px `column-width` set in JS, and measurement deferred until
   `window.load` + `document.fonts.ready` + a `requestAnimationFrame` stability poll; only then is
   the saved position restored. Pagination geometry is forced with `!important` so author CSS can't
   break it.

## 4. What changed in this pass (code)

- **`ReflowReader`**: `WebViewAssetLoader` serving + empty-200 egress block (the root-cause fix);
  `fillMaxSize` WebView; position restore moved out of `onPageFinished` into the pager.
- **`ReaderAssets`**: rewritten pager — body-as-scroller geometry (`scrollLeft`/`clientWidth`),
  exact-px column width, settle-after-fonts/images + stability poll, debounced resize re-anchor,
  `!important` pagination geometry.
- **`HtmlSanitizer`** (EPUB) and **`HtmlContent`** (HTML): parse from **bytes** with jsoup's
  charset auto-detection and lenient HTML5 parser.
- **`EpubContent`**: zip **entry index** for forgiving resource resolution (decoded / case-
  insensitive / backslash); **OPF fallback** to any `*.opf`.
- **`EpubFormatHandler`**: **DRM detection** (encryption.xml AES/ADEPT → `DrmProtectedException`);
  OPF fallback for metadata/cover.
- **Reader error classification**: `ReaderRepository.open` returns `OpenOutcome` (Success /
  Failure with a specific message): missing file, unsupported format, DRM-protected,
  password-protected, corrupt, or generic — shown on the reader's error card with **Go back**.
- **Injected CSS**: image containment (`object-fit`, `box-sizing`, `break-inside`).

## 5. Planned follow-ups (next hardening commit)

Font de-obfuscation; CSP injection + `srcset`/`javascript:` handling; integer column snapping +
post-`load` repagination; fixed-layout EPUB3 path; PDF bitmap-byte clamping and API-35 password
support; CP437 zip-name fallback; synthesized TOC; a **malformed-book regression corpus** run in
CI ([takahashim/epub-check-test](https://github.com/takahashim/epub-check-test)).
