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

**Chosen design: Concept A — "Pup."** The book cover is a friendly puppy face (eyes, snout,
nose) with the left ear erect and the right ear folded into the dog-eared page fold. It is
now the canonical `logo.svg`, and the adaptive-icon layers are derived from it.

| Asset | File | Use |
|-------|------|-----|
| Primary logo | [`logo.svg`](logo.svg) | App store, web, splash, about screen |
| Adaptive icon — foreground | [`icon-foreground.svg`](icon-foreground.svg) | Android adaptive icon foreground (Pup art scaled into the safe zone) |
| Adaptive icon — background | [`icon-background.svg`](icon-background.svg) | Android adaptive icon background (full-bleed) |

Concept exploration is kept for reference: [`logo-concept-a.svg`](logo-concept-a.svg)
(selected) and [`logo-concept-b.svg`](logo-concept-b.svg) (minimal alternative).

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
