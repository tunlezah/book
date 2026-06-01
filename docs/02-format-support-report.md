# Deliverable 2 — Format Support Report

Covers all required formats: EPUB, EPUB3, PDF, MOBI, AZW3, FB2, TXT, HTML/HTM, DOCX, RTF,
CBZ, CBR. For each: popularity, rendering requirements, parsing challenges, memory
concerns, licensing implications, and available open-source libraries. A consolidated
comparison matrix and the v1 support tier decision follow.

---

## 1. Comparison Matrix

Legend — Popularity: ★ (niche) … ★★★★★ (ubiquitous). Reflow: Y = reflowable text,
N = fixed layout. Difficulty: parsing/rendering effort (Low/Med/High).

| Format | Pop. | Reflow | Container | Rendering need | Parsing challenge | Memory concern | License risk | OSS libraries |
|--------|------|--------|-----------|----------------|-------------------|----------------|--------------|---------------|
| **EPUB 2** | ★★★★★ | Y | ZIP (OCF) | XHTML+CSS via WebView | OPF/NCX parse, manifest/spine, broken zips | Inflated HTML per spine item; images | Low (open standard) | epublib, Readium, jsoup, custom |
| **EPUB 3** | ★★★★★ | Y | ZIP (OCF) | XHTML5+CSS3, nav.xhtml, MathML, media overlays, fixed-layout | Nav doc vs NCX, FXL, scripted content (disable), encryption (DRM) | Same + SVG/audio | Low (open) | Readium, epublib, custom |
| **PDF** | ★★★★★ | N | Binary | Page raster (Android PdfRenderer / Pdfium / MuPDF) | Damaged xref, encryption, huge pages | Large bitmaps per page; tiling for zoom | **MuPDF = AGPL** (risk); Pdfium = BSD; PdfRenderer = platform | `android.graphics.pdf.PdfRenderer` (free), PdfiumAndroid (BSD-ish), MuPDF (AGPL/commercial) |
| **MOBI** | ★★★★ | Y | PalmDOC | Decompress (PalmDOC/HUFF-CDIC) → HTML reflow | Old Amazon format, EXTH metadata, DRM on store copies | Decompressed text in memory | Format reverse-engineered; **no Amazon DRM** | mobi (libmobi, C, GPL/LGPL), kindle-unpack-derived, custom Kotlin |
| **AZW3 (KF8)** | ★★★ | Y | PalmDB+KF8 | KF8 = EPUB3-like HTML → reflow | Complex KF8 sections, EXTH, possible DRM | Similar to EPUB | Reverse-engineered; **no DRM bypass** | libmobi (supports KF8), custom |
| **FB2** | ★★★ (RU/East EU) | Y | Single XML (or .fb2.zip) | XML → styled text/HTML | Large single XML, embedded base64 binaries (covers/images) | Whole-doc XML; base64 image blobs | Low (open XML) | jsoup/XML pull parser, custom; FBReader engine reference |
| **TXT** | ★★★★ | Y | Plain | Encoding detect → paginate | Charset detection (UTF-8/16, CP1251, GBK), no structure | Trivial if streamed; large files need chunking | None | juniversalchardet/ICU4J for charset, custom |
| **HTML/HTM** | ★★★ | Y | Single file (+assets) | Direct WebView reflow | Arbitrary/malformed HTML, external resource refs, scripts | DOM size; remote resource fetch (must block) | Low | WebView, jsoup sanitize |
| **DOCX** | ★★★ | Y | ZIP (OOXML) | Parse `document.xml` → HTML reflow | Complex OOXML, styles, tables, images in zip | XML + media; large docs | Low (open OOXML) | Apache POI (heavy), docx4j, or custom lightweight OOXML→HTML |
| **RTF** | ★★ | Y | Plain (RTF control words) | Parse control words → HTML/styled text | Quirky legacy spec, nested groups, encodings | Token stream; usually small | Low | custom RTF parser; few maintained Android libs |
| **CBZ** | ★★★ | N | ZIP | Image pager (decode per page) | Just images in a zip; natural sort of entries | One/few large bitmaps at a time | Low | java.util.zip + image decode (Coil/BitmapFactory) |
| **CBR** | ★★★ | N | RAR | Image pager | **RAR is proprietary**; needs unrar | Same as CBZ | **unrar license is non-free/custom** (junrar is GPL reimpl) | junrar (GPL), or libarchive |

---

## 2. Per-Format Notes

### EPUB 2 / EPUB 3 — *primary format, P1*
- **Rendering:** EPUB is XHTML+CSS in a ZIP. The highest-fidelity, lowest-effort engine is
  a `WebView` rendering each spine item, paginated with CSS multi-column. We inject our own
  reading CSS (fonts, spacing, theme) on top of (optionally sanitized) publisher CSS.
- **Parsing:** Read `META-INF/container.xml` → OPF (`<metadata>`, `<manifest>`, `<spine>`).
  TOC from EPUB3 `nav.xhtml` (preferred) or EPUB2 NCX (fallback). Resolve relative hrefs.
- **EPUB3 extras:** fixed-layout (FXL) pages (treat like fixed pages), MathML (WebView
  handles via MathJax fallback if needed), media overlays (defer), scripted content
  (**disabled for security** — see security review).
- **Memory:** Load spine items lazily, one (plus neighbors) at a time; never inflate the
  whole book. Stream images from the zip via custom `WebViewAssetLoader`/content provider
  rather than extracting to disk.
- **Libraries:** Lightweight custom OPF/NCX/nav parser (jsoup or XmlPullParser) is preferred
  over heavyweight Readium for memory and APK size; Readium kept as reference.

### PDF — *fixed layout, P1 breadth*
- **Rendering:** `android.graphics.pdf.PdfRenderer` (API 21+) renders a page to a `Bitmap`.
  No reflow. Implement tiled rendering + downscaling for zoom; reuse bitmaps; render at
  device density, not full PDF resolution.
- **Memory:** A single A4 page at high DPI can be tens of MB. Render at view resolution,
  cache 1–2 pages, recycle aggressively.
- **Licensing:** Prefer the platform `PdfRenderer` (no extra license, smaller APK). **Avoid
  MuPDF in the default build** (AGPL → would force AGPL or a commercial license). Pdfium
  (BSD) is an optional future engine for better text extraction/selection.

### MOBI / AZW3 — *legacy Amazon, P2*
- **Rendering:** Decompress to HTML, then reuse the EPUB WebView reflow path. AZW3/KF8 is
  essentially EPUB3-like HTML once unpacked.
- **Challenges:** PalmDOC and HUFF/CDIC decompression; EXTH metadata records; KF8 section
  tables. **DRM-protected store files are not supported and will not be circumvented.**
- **Libraries:** `libmobi` (C, LGPL — usable via JNI with care) supports MOBI and KF8.
  A pure-Kotlin reimplementation of PalmDOC decompression + EXTH parsing avoids JNI/license
  friction for the common (unencrypted) case; evaluate during implementation.

### FB2 — *P2*
- Single XML document with a defined schema; images embedded as base64 `<binary>` elements
  (including the cover). Convert to HTML for the WebView path. Watch memory on large single
  XML files (stream with a pull parser; decode base64 images lazily).

### TXT — *P1 (trivial, high value)*
- Detect encoding (ICU4J `CharsetDetector` covers UTF-8/16, CP1251, GBK, Shift-JIS).
  Wrap in minimal HTML (preserve paragraph breaks) and reflow. Chunk very large files.

### HTML / HTM — *P2*
- Reflow directly in WebView after **sanitizing** (strip scripts, block remote resource
  loads). Single-file and asset-relative cases handled by the asset loader.

### DOCX — *P3*
- OOXML in a ZIP. Apache POI is powerful but **heavy (APK + memory)**. Prefer a
  lightweight custom `document.xml` → HTML transform covering paragraphs, runs, basic
  styles, lists, tables, and embedded images. Full POI considered only if needed.

### RTF — *P3*
- Legacy control-word format. Few maintained Android libraries; implement a focused parser
  (text, bold/italic, paragraphs, Unicode `\u`, embedded images) → HTML. Usually small files.

### CBZ — *P2 (comics)*
- ZIP of images. Natural-sort entries, decode one page at a time with Coil/BitmapFactory,
  downscale to viewport, prefetch neighbors. Low risk, low effort.

### CBR — *P3 (comics, license caveat)*
- RAR archive. The reference `unrar` source is under a restrictive custom license; **junrar**
  is a clean-room GPL reimplementation. Bundling GPL code has licensing implications for the
  app. **Decision: gate CBR behind a license review**; ship CBZ first. May offer CBR via an
  optional component or recommend converting to CBZ.

---

## 3. Licensing Summary (App-Level Implications)

| Library | License | Implication | Decision |
|---------|---------|-------------|----------|
| Android `PdfRenderer` | Platform (Apache via AOSP) | None | **Use for PDF v1** |
| MuPDF | AGPL / commercial | Would force AGPL or paid license | **Avoid in default build** |
| Pdfium (PdfiumAndroid) | BSD-style | Permissive | Optional future PDF upgrade |
| libmobi | LGPL | Dynamic-link/JNI OK with attribution; static needs care | Evaluate vs. pure-Kotlin |
| junrar (CBR) | GPL | Strong copyleft — affects whole app if linked | **Gate behind review; CBZ first** |
| jsoup | MIT | Permissive | Use for HTML/XML parsing |
| ICU4J | ICU (permissive) | Fine | Use for charset/collation |
| Apache POI | Apache 2.0 | Permissive but heavy | Avoid unless necessary |
| Coil | Apache 2.0 | Permissive | Use for image loading/cache |

The app itself should ship under a permissive license (e.g., Apache 2.0) **only if** all
linked components allow it. Any GPL/AGPL component (junrar, MuPDF) would dictate the app's
license — hence both are excluded or gated.

---

## 4. v1 Support Tiers (decision)

| Tier | Formats | Rationale |
|------|---------|-----------|
| **Tier 1 — launch, fully polished** | EPUB2, EPUB3, TXT, PDF, CBZ | Highest popularity + lowest license/effort risk; covers the vast majority of real libraries. EPUB is the flagship. |
| **Tier 2 — fast-follow** | FB2, HTML/HTM, MOBI, AZW3 | High value, moderate effort; reuse the reflow path. MOBI/AZW3 require the unpack work. |
| **Tier 3 — later / gated** | DOCX, RTF, CBR | Lower popularity and/or license/effort concerns (POI weight, junrar GPL). |

The architecture defines a `BookFormatHandler` extension point so adding Tier 2/3 formats
is additive, not a redesign (see [05-architecture.md](05-architecture.md)). All formats
route through one of three render paths: **reflow (WebView)**, **fixed page raster (PDF)**,
or **image pager (CBZ/CBR)**.
