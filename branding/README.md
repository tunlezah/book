# Dogear — Branding

## Name

**Dogear.**

- The universal "save my place" gesture — folding a page corner — said out loud as one
  clean, playful, memorable word. Instantly tied to the app's bookmark / "pick up where you
  left off" promise.
- Doubles as a pun the logo leans into: the product is **a book that's secretly a dog**,
  whose folded ear *is* the dog-eared page.

**Tagline:** *Pick up right where you left off.*

**Suggested package id:** `com.dogear.reader`

## Logo

The mark blends a **book with a dog**: one ear perked **up**, the other **folded over** like
a dog-eared page (the gold fold). Warm paper + gold-fold + ink palette echoes the Cream and
Sepia reading themes.

Two concepts are under review:

| Concept | File | Character |
|---------|------|-----------|
| **A — Pup** | [`logo-concept-a.svg`](logo-concept-a.svg) | The book cover is a full puppy face (eyes, snout, nose); left ear up, right ear folded. Friendly, characterful. |
| **B — Minimal** | [`logo-concept-b.svg`](logo-concept-b.svg) | A cleaner, iconic page whose two top corners read as ears (one up, one folded), with subtle dot eyes, a snout, and a couple of text lines. Reads as both book and dog; scales better to tiny sizes. |

Once a concept is chosen it becomes the canonical `logo.svg`, and the adaptive-icon layers
below are regenerated to match.

| Asset | File | Use |
|-------|------|-----|
| Primary logo | `logo.svg` | App store, web, splash, about screen |
| Adaptive icon — foreground | `icon-foreground.svg` | Android adaptive icon foreground (art in safe zone) |
| Adaptive icon — background | `icon-background.svg` | Android adaptive icon background (full-bleed) |

> Note: `logo.svg` and the `icon-*.svg` files currently hold the earlier page-only mark and
> will be replaced with the chosen book-dog concept.

### Palette
| Role | Hex |
|------|-----|
| Page light | `#FBF3E2` |
| Page shadow | `#F1E4C8` |
| Ear inner | `#E6D3AE` |
| Fold (gold) | `#E0AC4A` → `#B9822A` |
| Ink (outline/eyes/text) | `#5B4636` |
| Icon background | `#3C342A` → `#24201A` |

### Implementation notes (for Phase 0)
- Convert the chosen SVG to `VectorDrawable` XML; wire an `adaptive-icon` with the
  foreground/background layers (plus a **monochrome** layer for Android 13 themed icons).
- Provide legacy `mipmap` PNG fallbacks (48–192 dp) for pre-adaptive launchers.
- The folded-ear motif doubles as the in-app "bookmarked" indicator.
