# Deliverable 11a — Reader Engineering Research (Phase 3 pre-work)

Targeted research into how existing readers (Readium / epub.js, KOReader, Librera, and
PdfRenderer-based apps) fail at rendering, and the format-specific traps, so the Dogear reader
avoids them by design. Findings drive the concrete mitigations listed at the end.

---

## 1. Reflowable EPUB / HTML in a WebView (the flagship path)

### How pagination is done (and the standard technique)
Horizontal pagination is achieved with **CSS multi-column**: set the content column width to the
viewport width and a fixed height equal to the viewport, so content flows into side-by-side
"pages"; turn pages by translating/scrolling horizontally by one column step. Page count is
derived from `scrollWidth / step` where `step = columnWidth + columnGap`.
(Refs: WKWebView horizontal paging writeup; DEV.to web-based reader.)

### Documented failure modes in real readers
- **Reading position shifts / is lost on relayout.** Readium reported the position drifting
  backwards when switching single/double spreads, and being lost on window resize
  ([readium/readium-js-viewer#199](https://github.com/readium/readium-js-viewer/issues/199)).
  Root cause: anchoring to a **pixel/column offset** that becomes meaningless after reflow.
- **CSS columns break for RTL + vertical writing mode**
  ([readium/readium-js-viewer#51](https://github.com/readium/readium-js-viewer/issues/51)).
- **CFI/locator precision is hard.** Jumping to a CFI when the same text repeats on a page, or
  resolving an exact sentence, lands at the page start rather than the exact spot
  ([w3c/epub-specs#1050](https://github.com/w3c/epub-specs/issues/1050);
  [EPUB Locators](https://w3c.github.io/epub-specs/epub33/locators/)).
- **Blank/last page & gap artifacts.** Without care the last column is clipped or an extra blank
  page appears; community fix is a trailing sizing element and computing page count by rounding,
  not flooring. The deprecated `overflow:-webkit-paged-x` is tempting but unreliable — avoid it.
- **Images/tables overflow or split across columns**, producing half-images at page edges.
- **Font-size change reflows everything** — the page a reader was on no longer exists; position
  must be recomputed from a DOM anchor, not a remembered page number.

### Security tension (JS)
Column measurement and re-anchoring need JavaScript, but enabling JS would also run **publisher
scripts** in the page. Resolution: **strip author scripts/handlers and rewrite resource URLs at
load time** (sanitize each spine document), then enable JS solely for our injected, audited
pagination/locator code, with **all network and `file://` access blocked**. This reconciles the
Security Review (no publisher JS, no egress) with the need for measurement.

### Memory / leaks
WebView is a classic Android leak source: it can outlive its Activity and holds a context
reference; large content inflates heap. Mitigations: **destroy() the WebView on dispose**, remove
it from its parent first, null the JS bridge, load one spine item (+ neighbors) at a time, and
never inflate the whole book. (Refs: WebView leak gist/SO; AOSP guidance.)

---

## 2. PDF (fixed layout)

- **`PdfRenderer` is not thread-safe and allows only one open page at a time**; opening a page
  while another is open throws `IllegalStateException`
  ([PdfRenderer docs](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)).
  → Serialize all open/render/close on a **single dedicated thread** (or a `Mutex`), render the
  visible page on demand, prefetch neighbors sequentially.
- **Huge page bitmaps cause OOM.** Render at **view resolution** (cap the long side), fill white
  before `render()` (PDFs assume an opaque page), and **recycle** bitmaps. Cache only 1–2 pages.
- Encrypted PDFs: detect and surface as unsupported; never bypass.

---

## 3. Comics (CBZ) and archives

- A CBZ is a ZIP of images; decode **one page at a time** at viewport resolution with neighbor
  prefetch; never decode all pages. Natural-sort entries so `page2` precedes `page10` (already in
  `SafeZip`). Apply the same zip-bomb/oversize guards as EPUB.
- CBR (RAR) stays license-gated (junrar = GPL).

---

## 4. Immersive reading UI

- Hide system bars via **`WindowInsetsControllerCompat`** (sticky immersive); re-show on a top
  tap. Handle **display cutouts** (`shortEdges`) and apply insets to the *controls* (top bar /
  scrubber) so they don't sit under the status/nav bars when shown
  ([immersive](https://developer.android.com/develop/ui/views/layout/immersive),
  [edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge)).
- **Gesture-nav conflict:** left/right *swipes* collide with the system back-gesture edge zones.
  Our default page-turn is a **tap** (swipe is optional), so paging doesn't fight the back
  gesture; for any edge drag handles we'd use `systemGestureExclusionRects`. Avoid double-applying
  insets (a documented glitch when a component already pads for bars).

---

## 5. Locator strategy (our answer to the position-loss bugs)

Store a **DOM-anchored** locator, not a pixel offset (Architecture §5):
`spineIndex` + a CSS path/`charOffset` + a short `textSnippet`, plus `progression` (0..1) as a
fallback. On open or after any reflow (font/size/orientation change), re-find the anchor element
in the DOM and turn to **its** column; if the anchor can't be found (e.g. after a re-import),
fall back to `progression × pageCount`. This is exactly the failure Readium hit by trusting
offsets, and the W3C Locators model (page/anchor/CFI) we mirror.

---

## 6. Decisions applied in Phase 3

| Risk (from research) | Decision |
|----------------------|----------|
| Position lost on reflow/resize | DOM-anchored locator + progression fallback; recompute page after every relayout |
| Publisher JS / egress | Sanitize each spine doc (strip scripts/handlers, rewrite URLs); JS on only for our code; block network + file access; serve resources via `WebViewAssetLoader` |
| Blank/last page, gaps | Compute page count by rounding `scrollWidth/step`; clamp; explicit column-gap accounted for in step |
| Image/table overflow & splitting | Injected CSS: `img/svg/video { max-width/height; break-inside: avoid }`, `table/pre/figure { break-inside: avoid }` |
| RTL / vertical writing | Support LTR + RTL page progression now; vertical (CJK) writing mode deferred (documented limitation) |
| WebView leak | `destroy()` + detach + null bridge on dispose; one spine item at a time |
| PdfRenderer not thread-safe | Single-thread/`Mutex`-serialized open/render/close; view-resolution bitmaps; recycle; cache 1–2 |
| Comic memory | Decode one page at a time at viewport size; prefetch neighbors |
| Immersive insets/cutout/gestures | `WindowInsetsControllerCompat` sticky immersive; cutout shortEdges; tap (not swipe) default paging; insets applied only to controls |

These mitigations are implemented directly in the `:feature:reader` engines and documented inline.
