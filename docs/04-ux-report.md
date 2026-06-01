# Deliverable 4 — UX Report

Defines the experience: design language, library/shelf, cover behavior, reading screen,
navigation, controls, theming, and accessibility. Visual specifics (palettes, themes) are
concrete enough here to implement directly.

---

## 1. Design Language — "Quiet Material"

Material 3 components and layout, driven by curated static palettes with optional dynamic
color, and deliberately subdued motion (rationale in [01-research-report.md](01-research-report.md) §4).

- **Adaptive layout** via window size classes: Compact → single pane; Medium → single pane
  + wider margins; Expanded → list-detail two-pane (library list ⇄ detail; reader ⇄ TOC).
- **Navigation:** bottom navigation bar on compact; navigation rail on expanded width.
- **Shape:** 12–16 dp corners; **Motion:** short, optional; reading transitions instant by
  default.
- **Two theme systems, decoupled:** (a) *App chrome theme* (palette + light/dark/system for
  shelf, settings, dialogs, menus) and (b) *Reading theme* (page background/text for the
  reader only). A user can read in AMOLED Black while the library uses a light palette.

---

## 2. Library / Shelf

**Goals:** instant startup, buttery scrolling, cached covers, background indexing, DB search.

- **Views:** Grid (cover-forward, adaptive column count by width) and List (cover thumbnail +
  title/author/progress). Toggle persists.
- **Sort:** Recently read · Recently added · Title · Author · File size · Reading progress.
- **Filter:** Author · Format · Reading status · Collection · Tag (combinable; DB-backed).
- **Reading states:** Unread · Reading · Finished · Abandoned (auto-derived from progress
  with manual override; shown as a small badge/ribbon on covers).
- **Search:** Title/author/tags/metadata via Room FTS; results instant as you type.
- **Performance:** `LazyVerticalGrid`/`LazyColumn` with stable keys; covers loaded by Coil
  from the on-disk thumbnail cache off the main thread; placeholders shown immediately so
  **scrolling never blocks** (see [08-performance-strategy.md](08-performance-strategy.md)).
- **Detail screen:** cover, full metadata, description, progress, reading-time stats; actions
  (Read, edit metadata, replace cover, refresh metadata, manage collections/tags, delete).
- **Empty state:** clear call to action to import or start the web upload server.

---

## 3. Cover Management

- **Extraction in background** (WorkManager/coroutine indexer), never on the UI thread.
- **If a cover exists:** extract → generate optimized thumbnails (grid + list sizes, device
  density) → cache permanently on disk (keyed by content hash so re-imports reuse it).
- **If no cover exists:** generate a **template cover** from title + author using one of
  several style templates (color drawn deterministically from the title hash, typographic
  layout in the user's font). Looks intentional, not broken.
- **Cache:** disk-persistent across launches; sized for thousands of books; avoids
  regeneration via content-hash keys + an LRU memory layer for what's on screen.
- **Hard rule:** shelf scrolling never blocks while loading covers — placeholders + async.

---

## 4. Import

- File picker (single + multiple), folder import (tree URI via SAF), ZIP archive import
  (extract supported books with zip-bomb guards — see [07-security-review.md](07-security-review.md)),
  and drag-and-drop where the platform supports it.
- Sources: Downloads, internal storage, SD card, external/USB storage (all via Storage
  Access Framework so it works without broad storage permissions).
- Each import runs validation → metadata extraction → cover/thumbnail → DB entry → shelf
  refresh, with progress feedback and duplicate detection (content hash).

---

## 5. Web Upload System

A built-in **local-network** HTTP server (off by default).

- Controls: enable/disable, choose port, optional upload password (Basic/Bearer over the
  local network). The active URL (e.g. `http://192.168.x.x:PORT`) and a QR code are shown.
- Upload pipeline per file: **validate → extract metadata → extract cover → generate
  thumbnails → add DB entry → refresh shelf**; the book appears automatically.
- Supports progress indicators, concurrent uploads, duplicate detection, and clear error
  handling. Security (local-only binding, validation, path-traversal protection, safe file
  handling, zip-bomb protection) is specified in [07-security-review.md](07-security-review.md).

---

## 6. Reading Experience (the core)

**Default mode is distraction-free: no permanent controls.** Just the page, edge to edge
(minus typographic margins), with the status bar/nav bar hidden (immersive).

### Tap zones
```
┌─────────────────────────────────────────┐
│              TOP  → reading controls      │  (top band, ~15% height)
├───────────┬───────────────────┬───────────┤
│           │                   │           │
│  LEFT     │   CENTER (tap =    │   RIGHT   │
│  prev pg  │   toggle menus /   │   next pg │
│           │   nothing — config) │           │
│           │                   │           │
├───────────┴───────────────────┴───────────┤
│        BOTTOM → progress scrubber          │  (bottom band)
└─────────────────────────────────────────┘
```
- **Left tap:** previous page. **Right tap:** next page. (Mirrored for RTL books.)
- **Top tap:** open reading controls (top app bar + chapter info).
- **Bottom tap:** reveal the progress scrubber (drag to seek; shows % and chapter).
- Controls **auto-hide** after a short idle. Swipe and (optional) volume keys also page.
- Tap-zone behavior is configurable (e.g., center action, swap left/right) — but ships with
  sensible defaults so it works perfectly untouched.

### First-open tutorial overlay
On first opening a book: dim the content slightly and show a brief overlay illustrating the
tap zones, top menu, and bottom scrubber; **auto-dismiss after 3–4 seconds** (or on tap).
A setting enables/disables the overlay; it can be re-triggered from settings.

### Reading transitions
Instant by default. Page-turn animation is a user choice: **None (instant) / Slide / Fade /
Curl**, with None as the default for the fastest feel and lowest cost on low-end hardware.

---

## 7. Reading Controls (top menu)

Adjustable: **Font · Font size · Font weight · Line spacing · Paragraph spacing · Margins ·
Text alignment · Hyphenation · Page animation.** Plus: **orientation lock, brightness,
fullscreen toggle, keep-screen-awake.** Quick access to **TOC, bookmarks, highlights/notes,
in-book search, go to page/percentage.**

- Presented as a bottom sheet with grouped controls and live preview; "Advanced" section
  hides the less-common toggles (progressive disclosure — avoids Librera/Moon+ overload).
- **Brightness** is adjustable directly in the reader (overlay dim layer for below-system
  brightness, plus a system-brightness mode); see Screen Management.

---

## 8. Reading Themes (page)

Built-in page themes (background / text), all WCAG-checked for contrast:

| Theme | Background | Text | Notes |
|-------|-----------|------|-------|
| Pure White | `#FFFFFF` | `#111111` | Maximum brightness |
| Cream | `#FBF0D9` | `#5F4B32` | Warm, low glare |
| Sepia | `#F4ECD8` | `#5B4636` | Classic e-reader |
| Warm Paper | `#F2E8D5` | `#3D3526` | Softer than sepia |
| Newspaper | `#EDEAE2` | `#22211E` | Neutral, high legibility |
| Light Grey | `#E8E8E8` | `#1E1E1E` | Reduced white glare |
| Dark Grey | `#2A2A2A` | `#D6D2C8` | Low-light, not pure black |
| AMOLED Black | `#000000` | `#C9C5BC` | Power-saving on OLED |

- Modes: **Light / Dark / Follow system** select which theme is used per mode.
- **Custom themes:** user-defined background, text, link, and highlight colors; saved and
  selectable. Live preview while editing.

---

## 9. Application Theming (chrome)

- **Light / Dark / Follow system.**
- **≥ 10 curated palettes** affecting shelf, reader chrome, settings, dialogs, menus. Each is
  a full Material 3 tonal scheme (light + dark variants):

  1. Ink (neutral charcoal) · 2. Paper (warm neutral) · 3. Forest (green) · 4. Ocean (blue) ·
  5. Plum (purple) · 6. Ember (warm red/orange) · 7. Slate (cool grey-blue) · 8. Sand
  (tan/gold) · 9. Rose (muted pink) · 10. Teal (blue-green) · 11. High-Contrast (accessibility)
  · 12. Dynamic (Material You, Android 12+, optional).

- A **High-Contrast** palette plus AMOLED-friendly dark variants serve accessibility.

---

## 10. Reading Progress, TOC, Bookmarks, Highlights, Notes

- **Progress:** last position (CFI-like locator: spine index + element/char offset, or PDF
  page + scroll), last chapter, reading %, and reading duration tracked. Reopen *exactly*
  where the user stopped. Persistence survives crashes/power loss/force-kill via frequent,
  atomic writes (see [05](05-architecture.md)/[06](06-database-schema.md)).
- **TOC:** chapter navigation, go-to-page, go-to-percentage — all instant via the locator
  system.
- **Bookmarks:** unlimited, optionally named, searchable, with a manager.
- **Highlights:** select text, multiple colors, manager; locators stored robustly so they
  survive re-import where possible (anchored by text + offset, not just byte position).
- **Notes:** attached to highlights or standalone; manageable and exportable (e.g., Markdown).

---

## 11. Search

- **Library search:** title/author/tags/metadata, FTS-backed, instant.
- **In-book search:** fast scan of the current book's text with result navigation; indexed
  where practical (e.g., cached extracted text per spine item).

---

## 12. Screen Management

- **Brightness:** Reader brightness (in-reader override via window attributes + dim overlay)
  · System brightness · Automatic (sensor-driven).
- **Keep awake:** Never · While reading · While charging (window flag managed by reading
  state / charging broadcast).

---

## 13. Accessibility

- Dynamic text sizing (respects system font scale in UI; independent reader size control).
- Full TalkBack/screen-reader semantics on shelf, controls, and reader; meaningful content
  descriptions; reader exposes text for screen-reader linear reading.
- Keyboard navigation throughout; in the reader: **← →** page, **PgUp/PgDn** page,
  **Home/End** book/chapter start/end, **Space** next.
- **Volume buttons turn pages** (optional setting).
- High-contrast theme + the contrast-checked palettes above; large touch targets (≥ 48 dp).

---

## 14. Settings (centralized)

Theme · Color palette · Font (UI/reader) · Reader settings · Brightness behavior ·
Keep-awake behavior · Upload server controls · Upload password · Tutorial overlay toggle ·
Cache management (clear thumbnail/text caches, show sizes) · Backup management
(export/restore). Organized into clear sections with search.
