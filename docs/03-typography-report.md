# Deliverable 3 — Typography Report

A reading app lives and dies by its type. This document summarizes the legibility and
reading-speed research, the font-licensing constraints, and the curated set of bundled
fonts. All selected fonts are **free for bundling and commercial redistribution**
(SIL Open Font License 1.1, Apache 2.0, or equivalent), so they ship inside the APK and
any of them can be applied to the **shelf, app UI, and reader** (where the format supports
font overrides).

---

## 1. Research Summary

### Legibility & reading-speed findings
- **No single font is universally "fastest."** Controlled studies (e.g., readability and
  reading-speed literature, Nielsen Norman Group reviews, the Readability Consortium) find
  that *familiarity, x-height, letter disambiguation, and spacing* matter more than the
  serif/sans distinction. Net effect on speed is usually small; **comfort and error rate**
  are where good type wins.
- **Serif vs. sans on screen:** On modern high-DPI displays the historical anti-serif bias
  has largely evaporated. Both work; **reader choice** is the right call. We ship a strong
  mix and default to a screen-tuned serif.
- **High x-height + open apertures** improve legibility at small sizes and low contrast
  (e.g., dim/sepia reading). This favors fonts like Literata, Bitter, Inter, Lexend.
- **Letter disambiguation** (distinct `I l 1`, `O 0`, `rn` vs `m`) reduces errors —
  critical for accessibility. Atkinson Hyperlegible is engineered specifically for this.
- **Spacing and measure dominate.** Line length (~60–75 characters), line height
  (~1.4–1.6), and adequate paragraph spacing affect speed and fatigue more than the
  typeface itself. Dogear therefore exposes line spacing, paragraph spacing, and margins
  as first-class controls (see [04-ux-report.md](04-ux-report.md)).
- **Hinting & rendering:** Fonts with good hinting/instructions render cleanly at body
  sizes on mid-DPI tablets. We prefer well-hinted families and apply font-smoothing CSS.
- **Dyslexia:** Evidence for "dyslexia fonts" improving speed is mixed, but many users
  *prefer* them and report comfort. We include OpenDyslexic as an opt-in accessibility
  choice rather than a default.

### Font-licensing constraints
- **Must be OFL/Apache/free for redistribution** so we can bundle and embed in EPUB/HTML.
- The SIL OFL permits bundling, embedding, and even renaming-on-modification; it forbids
  selling the fonts standalone (not our use case). Google Fonts' library is overwhelmingly
  OFL/Apache, which is our primary sourcing channel.
- Atkinson Hyperlegible is OFL (Braille Institute). OpenDyslexic is OFL/free.
- We bundle only the weights/styles we use (Regular, Italic, Bold, Bold Italic, and a
  variable axis where available) to control APK size; consider on-demand download of
  extra families post-v1 if size becomes a concern.

---

## 2. The Bundled Font Set (15 + 2 accessibility)

Curated for a balanced range: screen-tuned serifs, classic book serifs, humanist
sans-serifs for UI, plus accessibility specialists. The **default reader font is Literata**
(designed expressly for long-form on-screen reading); the **default UI font is Inter**.

| # | Font | Class | License | Why chosen / reading characteristics | Perf notes |
|---|------|-------|---------|----------------------------------------|------------|
| 1 | **Literata** ⭐default reader | Serif | OFL | Designed for Google Play Books long-form reading; large x-height, sturdy, low-fatigue. Variable weight. | Variable font → one file, several weights |
| 2 | **Bitter** | Slab serif | OFL | Slab built specifically for screen comfort; high legibility at body sizes. | Well hinted |
| 3 | **Merriweather** | Serif | OFL | Very popular ebook serif; large x-height, sturdy serifs, excellent at small sizes. | Slightly larger glyphs |
| 4 | **Source Serif 4** | Serif | OFL | Adobe's screen serif; clean, neutral, pairs with Source Sans. Variable. | Variable |
| 5 | **Lora** | Serif | OFL | Contemporary, calligraphic stress; pleasant for fiction. | Good hinting |
| 6 | **Noto Serif** | Serif | OFL | Massive language coverage (CJK/Cyrillic/Greek/etc.) — our fallback for multilingual books. | Large family; subset |
| 7 | **PT Serif** | Serif | OFL | Designed with Cyrillic parity; good for FB2/Russian texts. | Compact |
| 8 | **Crimson Pro** | Old-style serif | OFL | Book-like, elegant for literature; variable weight. | Variable |
| 9 | **EB Garamond** | Old-style serif | OFL | Classic Garamond revival; beautiful for long fiction; lower x-height (offer larger default size). | Subset |
| 10 | **Vollkorn** | Serif | OFL | "For body text"; warm, robust, designed for immersive reading. | Well hinted |
| 11 | **Charter (Bitstream Charter)** | Transitional serif | Bitstream free license (redistributable) | Legendary screen/print body face (Butterick favorite); economical, sharp at low DPI. | Tiny, fast |
| 12 | **Source Sans 3** | Humanist sans | OFL | Clean sans body option; great for non-fiction/technical. Variable. | Variable |
| 13 | **Inter** ⭐default UI | Humanist sans | OFL | Engineered for screens/UI; huge x-height, superb at small sizes; our app-chrome font. | Variable; excellent hinting |
| 14 | **Lexend** | Sans | OFL | Designed around reading-proficiency research; expandable spacing; reduces visual stress for some readers. | Variable |
| 15 | **Atkinson Hyperlegible** | Accessible sans | OFL | Engineered by Braille Institute for maximum letter disambiguation and low-vision legibility. | Moderate size |
| A1 | **OpenDyslexic** | Accessibility | OFL/free | Weighted bottoms to reduce letter flipping; opt-in for dyslexic readers. | Larger glyphs |
| A2 | **Noto Sans** | Sans fallback | OFL | Multilingual sans fallback to guarantee glyph coverage in UI and reader. | Subset |

> The set spans screen-serifs (Literata, Bitter, Source Serif, Merriweather), classic book
> serifs (EB Garamond, Crimson Pro, Charter, Vollkorn, Lora), Cyrillic-strong faces (PT
> Serif, Noto Serif), humanist sans for UI/non-fiction (Inter, Source Sans 3, Lexend), and
> accessibility specialists (Atkinson Hyperlegible, OpenDyslexic) — covering every common
> reading preference and language need while every file is freely bundleable.

---

## 3. Application of Fonts

| Surface | Behavior |
|---------|----------|
| **Reader** | User-selected reader font applied via injected CSS `@font-face` + `font-family !important` override (toggleable to respect publisher fonts for EPUB that bundles its own). Applies to EPUB/EPUB3/HTML/FB2/TXT/MOBI/AZW3/DOCX/RTF (all reflowable). **Not** PDF/CBZ/CBR (fixed/image — font override is N/A). |
| **Shelf** | Titles/authors rendered in the user-selected font (mapped to a Compose `FontFamily`). |
| **App UI / Settings / Dialogs / Menus** | Default Inter; user may switch the global UI font to any bundled family. |

A single `FontRegistry` maps each bundled family to (a) a Compose `FontFamily` for UI/shelf
and (b) an embeddable asset URL for the reader's `@font-face`. This keeps font selection
consistent across all three surfaces from one source of truth.

---

## 4. Performance Considerations

- **Variable fonts** (Literata, Inter, Source Serif/Sans, Crimson Pro, Lexend) provide many
  weights from one file → smaller APK and fewer font assets to load.
- **Subset** large families (Noto Serif/Sans) to the scripts we realistically need, or load
  full coverage lazily for multilingual books.
- **Lazy load** reader `@font-face` assets through the WebView asset loader; UI fonts are
  loaded on demand by Compose's `FontFamily` resolver and cached.
- **Estimated footprint:** with variable fonts + subsetting, the bundled set targets roughly
  6–10 MB of the APK — measured and tracked as a build budget (see
  [08-performance-strategy.md](08-performance-strategy.md)).
- **Font weight control:** because several families are variable, the "Font weight" reading
  control maps to the font's weight axis when available, otherwise snaps to bundled
  Regular/Bold.
