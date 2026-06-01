# Deliverable 1 — Research Report

This report covers the competitive landscape, Android tablet UX conventions, and the
Material Design 2 vs. 3 analysis that yields Dogear's hybrid design language. Format
research has its own dedicated document ([02-format-support-report.md](02-format-support-report.md)).

---

## 1. Competitor Analysis

The matrix below is followed by per-app notes. The goal is not to clone any one reader
but to extract the patterns that work and avoid the friction that hurts.

| App | Engine / Rendering | Strengths | Weaknesses | Library model | Key UX pattern to borrow |
|-----|--------------------|-----------|------------|---------------|--------------------------|
| **Kindle** | Proprietary, reflowable + KFX | Polished, fast, excellent typography, Whispersync | Closed ecosystem, sideloading clunky, no EPUB without conversion, heavy app | Cloud-first grid | Distraction-free reading; tap zones; clean type menu |
| **Kobo** | Adobe RMSDK + custom | Great EPUB support, statistics, Pocket integration | Slower UI, account-centric, bloated | Cloud + local grid | Reading statistics; beautiful serif defaults |
| **Moon+ Reader** | Custom HTML/CSS engine | Hugely configurable, many formats, themes, gestures | Cluttered UI, dated visuals, ads in free tier | Local-first shelf | Tap-zone customization; theme variety; auto-scroll |
| **ReadEra** | Custom (incl. PDF/MOBI/DOCX) | Fast, free, no ads, clean, great format coverage | Limited typography control, no web upload, basic theming | Local-first, smart collections | Speed; minimal chrome; "to read / read / favorites" states |
| **PocketBook** | Adobe-based | Good format coverage, sync, audiobooks | UI inconsistent across versions, account nudges | Cloud + local | Format breadth; per-book settings |
| **Librera** | Custom + MuPDF for PDF | Very configurable, free, many formats, TTS | Overwhelming settings, inconsistent visuals | Local-first | Per-format engines; deep gesture config |
| **KOReader** | MuPDF + crengine (koreader) | Best-in-class rendering control, e-ink optimized, open source | Power-user UI, steep learning curve, not Material | Local file browser | crengine for reflow; dictionary/lookup depth; on-device statistics |

### Synthesized takeaways

**What the best readers get right (and Dogear adopts):**

1. **Reading is sacred.** Kindle, Kobo, and ReadEra all default to a clean page with no
   permanent chrome. Dogear mandates a distraction-free default with tap zones.
2. **Local-first is faster and more private.** ReadEra and KOReader prove a snappy,
   account-free, offline-first experience is a competitive advantage. Dogear is
   offline-first; cloud is a *future* extension point only.
3. **Reading states matter.** ReadEra's to-read/reading/finished model maps directly to
   our required Unread/Reading/Finished/Abandoned states.
4. **Rendering engine choice defines quality.** KOReader's crengine and Moon+'s custom
   engine show that EPUB reflow quality is the differentiator. Dogear invests heavily
   in a WebView-based reflow engine with paginated CSS columns (see UX + Architecture).

**What they get wrong (and Dogear avoids):**

1. **Settings overload** (Librera, Moon+). Dogear uses progressive disclosure: a small,
   well-curated reading menu with an "Advanced" section, not a wall of toggles.
2. **Account gates and cloud nudges** (Kindle, Kobo, PocketBook). Dogear never requires
   an account.
3. **Dated visuals** (Moon+, older Librera). Dogear uses a modern Material 3 surface with
   careful restraint.
4. **Heavyweight apps** (Kindle). Dogear targets a small APK and a low memory ceiling.

---

## 2. Rendering Approaches Observed

- **WebView / HTML-CSS reflow** (Moon+, ReadEra-style): Best fidelity for EPUB/HTML
  because EPUB *is* XHTML+CSS. Pagination via CSS multi-column (`column-width: 100vw`)
  with horizontal translation is the standard high-performance technique. This is
  Dogear's primary EPUB path.
- **Native canvas rendering** (KOReader crengine): Maximum control and e-ink performance,
  but enormous implementation cost and weaker CSS fidelity. Out of scope for v1.
- **PDF page rasterization** (MuPDF / Pdfium): PDFs are fixed-layout; rendered as bitmaps
  per page with tiling for zoom. Dogear uses Android's built-in `PdfRenderer` for v1
  with Pdfium/MuPDF considered as an optional upgrade (license permitting).

Decision: **WebView reflow for all reflowable formats; PdfRenderer for PDF; bitmap pager
for CBZ/CBR.** Details and rationale in [05-architecture.md](05-architecture.md) and
[02-format-support-report.md](02-format-support-report.md).

---

## 3. Android Tablet UX Research

8-inch tablets and foldables occupy a middle ground between phone and large tablet. Key
findings driving Dogear's layout:

1. **Canonical breakpoints (Material window size classes):**
   - *Compact* width (< 600 dp): phones, folded foldables → single-pane.
   - *Medium* width (600–840 dp): most 8-inch tablets in portrait, large phones landscape
     → single-pane with wider margins, or list-detail where it adds value.
   - *Expanded* width (≥ 840 dp): tablets in landscape, unfolded foldables → list-detail
     two-pane (library list + book detail; reader + TOC drawer).
2. **Reading column width is a typography problem, not a screen problem.** Even on a wide
   screen, body text should hold ~60–75 characters per line. Dogear caps measure with
   responsive margins rather than letting lines run edge-to-edge. On expanded width it
   offers an optional **two-column page spread** (like a physical book).
3. **Reachability:** On 8-inch tablets held two-handed, top corners are hard to reach.
   Primary actions live at the bottom or as edge taps. The reader's top menu is opened by
   a top-zone tap rather than requiring a reach to a fixed top bar.
4. **Orientation:** Tablets are used in both orientations roughly equally; phones skew
   portrait. Dogear fully supports portrait/landscape and a per-book orientation lock.
5. **Input diversity:** Tablets attract keyboards and styluses. Dogear supports hardware
   keyboard navigation (arrows, PgUp/PgDn, Home/End, Space) and volume-key paging.
6. **Foldables:** Handle configuration changes without losing reading position; respect
   fold posture (tabletop / book mode) by using the hinge as a natural two-page gutter
   when the `WindowLayoutInfo` reports a vertical fold.

---

## 4. Material Design 2 vs. 3 — and Dogear's Hybrid

| Dimension | Material 2 (MD2) | Material 3 (MD3) | Dogear hybrid choice |
|-----------|------------------|------------------|------------------------|
| Color | Fixed primary/accent | Dynamic color (Material You), tonal palettes | **MD3 tonal palettes** as the system, but **10+ curated static palettes** shipped so older devices and users without dynamic color get identical, intentional results. Dynamic color offered as one optional palette on Android 12+. |
| Shape | Subtle rounding | Larger, expressive rounding | Moderate rounding (12–16 dp) — modern but not cartoonish; restrained for a reading app. |
| Components | Contained buttons, app bars | Filled/tonal/outlined buttons, navigation rail, bottom sheets | MD3 components via Compose Material3, but with conservative emphasis. |
| Elevation | Shadow-based | Tonal (surface tint) elevation | Tonal elevation, with a flat option for AMOLED/high-contrast. |
| Motion | Standard | Expressive, springy | Restrained motion; reading transitions are instant by default, decoration minimized. |
| Density | Comfortable | Comfortable + adaptive | Slightly denser on tablets to use space well. |

**Hybrid design language ("Quiet Material"):** Material 3 component set and layout system,
driven by **curated static palettes** (so the experience is consistent and intentional on
every device, including older ones), with optional Material You dynamic color on capable
hardware. Motion is deliberately subdued. The reading surface is fully themeable and
decoupled from the app chrome theme. This satisfies "modern yet familiar."

The full palette set, reading themes, and component specs are in [04-ux-report.md](04-ux-report.md).

---

## 5. Library Management Strategies Observed

- **Smart auto-collections** (ReadEra): to-read/reading/finished derived from progress.
  Dogear adopts reading states with both automatic transitions and manual override.
- **User collections + tags** (Calibre-style, Librera): Dogear supports both collections
  (folders) and free-form tags, with DB-backed filtering.
- **Series grouping** (Kobo, Calibre): Dogear stores series metadata for future grouping;
  v1 surfaces it in detail and sort.
- **Fast search**: All competitors that feel fast use indexed search. Dogear uses Room
  + FTS for instant library and in-book search where practical.

---

## 6. Conclusions Feeding Later Phases

1. EPUB-first, WebView-reflow rendering engine is the single most important technical bet.
2. Offline-first, account-free, local library is both a UX and privacy advantage.
3. A small curated settings surface beats Librera/Moon+ maximalism.
4. Distraction-free reading with tap zones is table stakes; we differentiate on **speed,
   memory, and polish** rather than feature count.
5. The design language is MD3 components + curated palettes + subdued motion.

These conclusions are non-negotiable inputs to the architecture, UX, and performance docs.
