# Deliverable 7 — Security Review

Dogear ingests untrusted files (downloaded books, web uploads) and runs an optional local
network server. This document is the threat model and the mitigations applied during
implementation. Principle: **treat every book file and every upload as hostile.**

---

## 1. Assets & Trust Boundaries
- **Assets:** the user's library/files, reading data, the device itself (CPU/RAM/storage),
  and the local network.
- **Trust boundaries:** (a) file → parser, (b) network → upload server, (c) book content →
  WebView (active content), (d) archive → extractor.
- **Adversary:** a malicious book file, a malicious upload over the LAN, or malformed content
  designed to crash/exhaust/exfiltrate.

---

## 2. Threats & Mitigations by Surface

### A. EPUB / HTML / active content (WebView)
| Threat | Mitigation |
|--------|------------|
| Malicious JavaScript in EPUB/HTML (data exfil, navigation, fingerprinting) | **Publisher scripts are stripped** at load time (jsoup removes `<script>`/`iframe`/`object`/`embed` and all `on*`/`javascript:` handlers); the WebView's JS engine is then enabled **solely for our injected, audited pagination/locator code**. No author script ever executes. (Implemented in `HtmlSanitizer` + `ReflowReader`; see Reader Engineering Research §1.) |
| External resource loads (tracking pixels, SSRF-style fetches, phoning home) | Block all network in the reader: `WebViewClient` denies non-local schemes; resources served **only** via `WebViewAssetLoader`/content provider from inside the book. No `http(s)` egress. |
| `file://` access / local file disclosure | `setAllowFileAccess(false)`, `setAllowFileAccessFromFileURLs(false)`, `setAllowUniversalAccessFromFileURLs(false)`; serve via virtual `https://appassets/` loader. |
| Malicious CSS (resource exhaustion, huge layouts) | Sanitize/limit injected CSS; cap document size per spine item; render lazily. |
| `@JavascriptInterface` abuse | Only a single, minimal bridge exposed; methods validate all input; no reflection/file APIs reachable. |
| Autoplay media / media overlays | Disabled in v1. |

### B. Archives (EPUB/CBZ/ZIP-import/DOCX/FB2.zip)
| Threat | Mitigation |
|--------|------------|
| **Zip bombs** (compression-ratio & nested) | Enforce limits: max **total uncompressed size**, max **per-entry size**, max **entry count**, and max **compression ratio** per entry. Abort + reject on breach. Stream-decompress with running totals; never trust the header size. |
| **Path traversal / Zip Slip** (`../`, absolute paths, symlinks) | Canonicalize every entry path; reject entries that escape the target dir; reject absolute paths and `..` segments; never preserve symlinks. |
| Decompression to disk filling storage | Prefer streaming from the archive without full extraction; when extraction is required, enforce a disk-budget and clean up on failure. |
| Malicious entry names (very long, control chars, reserved) | Sanitize/normalize filenames; bound length. |

### C. PDF
| Threat | Mitigation |
|--------|------------|
| Malformed/huge PDFs (parser crash, OOM via giant pages) | Use platform `PdfRenderer` (sandboxed, maintained by AOSP); render at **view resolution**, cap page bitmap dimensions, recycle. Catch and contain native exceptions → show a friendly error, don't crash. |
| Encrypted PDFs | Detect and prompt/skip; never attempt to bypass protection. |
| Embedded JS/actions | `PdfRenderer` does not execute PDF JavaScript — inherently safer than full PDF engines. |

### D. MOBI / AZW3 / FB2 / RTF / DOCX parsers
| Threat | Mitigation |
|--------|------------|
| Decompression bombs (PalmDOC/HUFF-CDIC), XML entity expansion (**billion laughs**) in FB2/DOCX/OPF | Disable DTDs/external entities in all XML parsers (`FEATURE_SECURE_PROCESSING`, no `DOCTYPE`); bound output sizes for decompression. |
| Malformed records → crashes | Defensive parsing with bounds checks; wrap in `runCatching`; reject rather than crash. |
| **DRM** | Not supported and **not circumvented** — by policy and to avoid legal risk. DRM'd files are detected and surfaced as unsupported. |

### E. Web Upload Server (the largest network surface)
| Threat | Mitigation |
|--------|------------|
| Exposure beyond LAN / WAN reachability | **Bind to the local network only**; do not bind public interfaces; off by default; clear in-app indication when running; auto-stop on app background/timeout option. |
| Unauthorized upload | Optional **password** (constant-time comparison); auth required before accepting a body when set. Consider a per-session token shown in-app/QR. |
| Path traversal in upload filename | Ignore client-supplied paths entirely; generate safe internal storage names; sanitize display names. |
| Oversized/zip-bomb uploads | Enforce max request size and per-file size **before** writing; apply the archive limits from (B) to uploaded archives. |
| Malicious file masquerading (wrong extension) | **Validate by content** (magic-byte sniff via `probe()`), not extension; reject unknown/unsupported types. |
| Resource exhaustion (many concurrent uploads) | Cap concurrent connections/uploads; bounded thread pool; request timeouts. |
| CSRF / browser-based abuse of the local server | Restrict to expected methods; require token/password; minimal, no-cookie endpoints; set restrictive CORS (no `*` with credentials). |
| Cleartext on LAN | Document that it's local-only; consider self-signed TLS option later. No credentials beyond the optional upload password traverse it. |

### F. General app
| Threat | Mitigation |
|--------|------------|
| Insecure storage of library/notes | App-private storage; SAF for user files (no broad storage permission). No secrets stored; upload password stored hashed/obfuscated in DataStore, not plaintext logs. |
| Logging sensitive data | No PII/credential logging; release builds strip verbose logs. |
| Backup leakage | `allowBackup` reviewed; user-initiated backup archive is explicit and local. |
| Dependency vulnerabilities | Keep deps minimal and updated; avoid AGPL/GPL (license risk) and unmaintained native libs; run dependency scanning in CI. |
| Tapjacking/overlays on reader controls | `filterTouchesWhenObscured` on sensitive controls. |

---

## 3. Resource-Exhaustion Budget (enforced limits)
Concrete caps (tunable constants, enforced in code and covered by tests):
- Archive: ≤ N entries, ≤ X MB total uncompressed, ≤ Y MB/entry, ≤ Z:1 ratio.
- Reflow: ≤ per-spine-item size; lazy single-item load.
- PDF/comic: bitmap dimension cap at viewport×density; ≤ 1–2 cached pages.
- Upload: ≤ request size, ≤ concurrent connections, request timeout.
Exceeding any limit → graceful rejection with a clear message, never a crash or hang.

---

## 4. Testing the Mitigations
A dedicated **malicious-input test corpus** (see [09-testing-strategy.md](09-testing-strategy.md)):
zip bombs, zip-slip archives, billion-laughs XML, truncated/corrupt EPUB/PDF/MOBI, oversized
uploads, traversal filenames, JS-laden EPUB. Each must be **rejected safely** (no crash, no
file escape, bounded memory/time). Upload-server tests assert local-only binding and auth.

---

## 5. Out of Scope (by policy)
- Circumventing any DRM.
- Acting as an attack tool, scanning networks, or non-local serving.
These are explicitly excluded; the upload server is a convenience for the user's own LAN.
