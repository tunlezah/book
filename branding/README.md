# Earmark — Branding

## Name

**Earmark.**

- A real English word meaning *to set aside / mark for a purpose* — exactly what a reader
  does with a book and a place in it.
- Carries the imagery of a **dog-eared page corner**, the oldest bookmark in the world,
  tying directly to the app's bookmark/"pick up where you left off" promise.
- Reads as one clean, professional, memorable word — witty without being cute.

**Tagline:** *Pick up right where you left off.*

**Suggested package id:** `com.earmark.reader`

## Logo

The mark is a **book page with a folded top-right corner** (the earmark), with a few text
lines suggesting reading. Warm paper + ink palette echoes the Cream/Sepia reading themes.

| Asset | File | Use |
|-------|------|-----|
| Primary logo | [`logo.svg`](logo.svg) | App store, web, splash, about screen |
| Adaptive icon — foreground | [`icon-foreground.svg`](icon-foreground.svg) | Android adaptive icon foreground layer (art kept in safe zone) |
| Adaptive icon — background | [`icon-background.svg`](icon-background.svg) | Android adaptive icon background layer (full-bleed) |

### Palette
| Role | Hex |
|------|-----|
| Page light | `#FBF3E2` |
| Page shadow | `#F2E6CC` |
| Fold (gold) | `#D9A441` → `#B9822A` |
| Ink (outline/text) | `#5B4636` |
| Icon background | `#3C342A` → `#24201A` |

### Implementation notes (for Phase 0)
- Convert these SVGs to `VectorDrawable` XML and wire an `adaptive-icon` with the
  foreground/background layers (and a monochrome layer for Android 13 themed icons).
- Provide legacy `mipmap` PNG fallbacks (48–192 dp) for pre-adaptive launchers.
- The fold motif doubles nicely as an in-app "bookmarked" indicator.
