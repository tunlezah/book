# Dogear

**A premium, distraction-free ebook reader for Android tablets and phones.**

> *Dogear* — verb: to set aside, to mark a page for what matters. The name carries
> the imagery of a dog-eared page corner (the oldest bookmark in the world) while
> reading as a clean, single English word. Professional, witty, and instantly tied
> to the product's bookmarking and "pick up where you left off" promise.

---

## Status

🟡 **Planning phase.** No application code has been written yet.

Per the project mandate, implementation is gated behind a complete set of research,
architecture, design, security, performance, and planning documents. Those documents
live in [`docs/`](docs/) and are listed below. Implementation (production source,
build instructions, tests, benchmarks, user docs) begins only after these are approved.

## Vision

Dogear is built around four words: **fast, light, reliable, distraction-free.**

It targets **Android 13 on 8-inch tablets** as the primary device, while remaining
fully optimized for phones and foldables. It is engineered to run smoothly on
low-end hardware (2–4 GB RAM), open a 1,000-book library in under two seconds, and
present an EPUB reading experience that competes with Kindle, Kobo, Moon+ Reader,
ReadEra, PocketBook, Librera, and KOReader.

## Priorities (drive every design decision)

| Priority | Goals |
|----------|-------|
| **P1** | Fast shelf loading · Excellent EPUB rendering · Extremely low memory · Automatic cover caching · Responsive UI |
| **P2** | Built-in web upload · Tap-zone navigation · Reading position persistence · Brightness controls · Keep-screen-awake |
| **P3** | Advanced library management · Typography customization · Highlights · Notes · Bookmarks |

## Planning Deliverables

| # | Document | Description |
|---|----------|-------------|
| 1 | [Research Report](docs/01-research-report.md) | Competitor analysis, Android tablet UX, Material 2/3 hybrid design language |
| 2 | [Format Support Report](docs/02-format-support-report.md) | EPUB/PDF/MOBI/AZW3/FB2/TXT/HTML/DOCX/RTF/CBZ/CBR matrix |
| 3 | [Typography Report](docs/03-typography-report.md) | Legibility research and the 15+ bundled fonts |
| 4 | [UX Report](docs/04-ux-report.md) | Reading experience, navigation, library, theming |
| 5 | [Architecture Document](docs/05-architecture.md) | Kotlin/Compose/MVVM/Room module and layer design |
| 6 | [Database Schema](docs/06-database-schema.md) | Room entities, indices, migrations, FTS |
| 7 | [Security Review](docs/07-security-review.md) | Format, archive, upload-server threat model and mitigations |
| 8 | [Performance Strategy](docs/08-performance-strategy.md) | Targets, budgets, and techniques for low-end devices |
| 9 | [Testing Strategy](docs/09-testing-strategy.md) | Unit/integration/UI/import/DB/performance testing |
| 10 | [Implementation Plan](docs/10-implementation-plan.md) | Phased, milestone-based build plan |

## Branding

The app name, logo concept, and icon assets are documented in
[`branding/`](branding/), including a vector logo and adaptive-icon foreground/background.

## License

To be finalized before public release. All bundled fonts are OFL/Apache/free-licensed
(see the [Typography Report](docs/03-typography-report.md)). Format-handling libraries
are selected for license compatibility (see the [Format Support Report](docs/02-format-support-report.md)).
