package com.dogear.reader.feature.reader.reflow

/** Typography + theme inputs for the injected reading CSS. Colors are pre-formatted CSS hex. */
data class ReflowStyle(
    val fontFamilyCss: String,
    val fontSizePx: Int = 18,
    val fontWeight: Int = 400,
    val lineHeight: Float = 1.5f,
    val paragraphSpacingEm: Float = 0.8f,
    val horizontalMarginPx: Int = 24,
    val verticalMarginPx: Int = 24,
    val textColor: String = "#5F4B32",
    val backgroundColor: String = "#FBF0D9",
    val linkColor: String = "#8A5A2B",
    val textAlign: String = "left",
    val hyphens: Boolean = true,
)

/**
 * Builds the page CSS + pagination JS and injects them into a sanitized spine document.
 *
 * Pagination technique (CSS multi-column, as used by epub.js / Readium):
 *  - The **body** is the paginated *and* scrolled element. It carries the horizontal reading
 *    margin as `margin` (not padding) and **zero horizontal padding**, so its content box width
 *    equals the column pitch exactly — no `innerWidth`-vs-padding drift.
 *  - `column-gap: 0` and `column-width` is set in JS to the body's exact integer `clientWidth`,
 *    guaranteeing exactly one column per page and `pitch === clientWidth`.
 *  - We page by setting `body.scrollLeft` (the body is the `overflow:hidden` scroll container —
 *    `window.scrollTo` would be a no-op here), and `pageCount = round(scrollWidth / clientWidth)`.
 *  - Measurement happens only **after** `window.load` *and* `document.fonts.ready`, then a short
 *    stability poll, because web fonts and late images reflow the text and change `scrollWidth`.
 *    Only once the layout is stable do we restore the saved position — this is what fixes the
 *    "text flashes once then blank, can't page" symptom.
 */
internal object ReaderAssets {

    /**
     * @param pendingFraction position (0..1 within this document) to restore once layout is stable.
     */
    fun buildHtml(
        bodyHtml: String,
        style: ReflowStyle,
        smoothPaging: Boolean = false,
        pendingFraction: Float = 0f,
    ): String {
        val css = css(style)
        val styleTag = "<style id=\"dogear-style\">$css</style>"
        val boot = "window.__dogearSmooth=$smoothPaging;window.__dogearPending=$pendingFraction;"
        val scriptTag = "<script id=\"dogear-pager\">$boot\n$PAGINATION_JS</script>"

        var html = bodyHtml
        html = if (html.contains("</head>", ignoreCase = true)) {
            html.replaceFirst(Regex("(?i)</head>"), "$styleTag</head>")
        } else {
            "$styleTag$html"
        }
        html = if (html.contains("</body>", ignoreCase = true)) {
            html.replaceFirst(Regex("(?i)</body>"), "$scriptTag</body>")
        } else {
            "$html$scriptTag"
        }
        return html
    }

    private fun css(s: ReflowStyle): String = """
        html {
            height: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
            -webkit-text-size-adjust: 100%;
            background: ${s.backgroundColor};
        }
        body {
            box-sizing: border-box !important;
            height: 100% !important;
            min-height: 100% !important;
            max-height: 100% !important;
            /* Horizontal reading margin lives in `margin` so it does NOT enter the column pitch;
               vertical margin is padding (it doesn't affect horizontal geometry). The geometry
               is forced with !important so author stylesheets can't break pagination. */
            margin: 0 ${s.horizontalMarginPx}px !important;
            padding: ${s.verticalMarginPx}px 0 !important;
            /* column-width is overwritten in JS with the exact clientWidth; this is a fallback. */
            column-width: 100% !important;
            -webkit-column-width: 100% !important;
            column-count: auto !important;
            -webkit-column-count: auto !important;
            column-gap: 0 !important;
            -webkit-column-gap: 0 !important;
            column-fill: auto !important;
            -webkit-column-fill: auto !important;
            overflow: hidden !important;
            font-family: ${s.fontFamilyCss};
            font-size: ${s.fontSizePx}px;
            font-weight: ${s.fontWeight};
            line-height: ${s.lineHeight};
            color: ${s.textColor};
            background: ${s.backgroundColor};
            text-align: ${s.textAlign};
            word-wrap: break-word;
            overflow-wrap: break-word;
            -webkit-hyphens: ${if (s.hyphens) "auto" else "none"};
            hyphens: ${if (s.hyphens) "auto" else "none"};
        }
        img, svg, video {
            max-width: 100% !important;
            max-height: 96vh !important;
            height: auto;
            object-fit: contain;
            box-sizing: border-box;
            break-inside: avoid;
            -webkit-column-break-inside: avoid;
            page-break-inside: avoid;
        }
        figure { margin: 0; }
        table, pre, figure, blockquote, h1, h2, h3 {
            break-inside: avoid;
            -webkit-column-break-inside: avoid;
        }
        a { color: ${s.linkColor}; text-decoration: none; }
        p { margin: 0 0 ${s.paragraphSpacingEm}em 0; }
    """.trimIndent()

    /**
     * Page measurement and navigation. The body is both the multi-column box and the horizontal
     * scroll container. All geometry is derived from the body's own `clientWidth`/`scrollWidth`
     * and `scrollLeft` — never `window.*` (the window doesn't scroll when body is overflow:hidden).
     */
    private val PAGINATION_JS = """
        (function () {
            var el = document.body;
            var ready = false;

            function pitch() {
                // With zero horizontal padding and column-gap:0, the column pitch is clientWidth.
                return Math.max(1, el.clientWidth);
            }
            function applyColumnWidth() {
                // setProperty(..., 'important') so the exact integer pitch beats the
                // `column-width: 100% !important` fallback in the injected stylesheet.
                var w = pitch() + 'px';
                el.style.setProperty('column-width', w, 'important');
                el.style.setProperty('-webkit-column-width', w, 'important');
            }
            function pageCount() {
                return Math.max(1, Math.round(el.scrollWidth / pitch()));
            }
            function currentPage() {
                return Math.max(0, Math.round(el.scrollLeft / pitch()));
            }
            function report() {
                if (window.DogearBridge) {
                    DogearBridge.onPaginated(pageCount(), currentPage());
                }
            }
            function goTo(p) {
                var c = pageCount();
                p = Math.max(0, Math.min(c - 1, p));
                var left = p * pitch();
                if (window.__dogearSmooth && el.scrollTo) {
                    el.scrollTo({ left: left, top: 0, behavior: 'smooth' });
                } else {
                    el.scrollLeft = left;
                }
                report();
            }

            window.DogearPager = {
                pageCount: pageCount,
                currentPage: currentPage,
                goTo: goTo,
                next: function () {
                    var c = pageCount(), p = currentPage();
                    if (p < c - 1) { goTo(p + 1); return true; }
                    return false;
                },
                prev: function () {
                    var p = currentPage();
                    if (p > 0) { goTo(p - 1); return true; }
                    return false;
                },
                goToFraction: function (f) {
                    var c = pageCount();
                    goTo(Math.round((f || 0) * (c - 1)));
                },
                recompute: function () { applyColumnWidth(); report(); }
            };

            // Re-measure when late images finish (they shift text and change scrollWidth).
            function watchImages(after) {
                var imgs = el.querySelectorAll('img');
                var pending = 0;
                for (var i = 0; i < imgs.length; i++) {
                    var img = imgs[i];
                    if (!img.complete) {
                        pending++;
                        img.addEventListener('load', after, { once: true });
                        img.addEventListener('error', after, { once: true });
                    }
                }
                return pending;
            }

            // Poll until scrollWidth stops changing (fonts/images settling), then settle once.
            function whenStable(done) {
                var last = -1, tries = 0;
                function tick() {
                    applyColumnWidth();
                    var w = el.scrollWidth;
                    if (w === last || tries > 20) { done(); return; }
                    last = w;
                    tries++;
                    requestAnimationFrame(tick);
                }
                requestAnimationFrame(tick);
            }

            function settle() {
                applyColumnWidth();
                whenStable(function () {
                    ready = true;
                    DogearPager.goToFraction(window.__dogearPending || 0);
                    report();
                });
            }

            function start() {
                // Wait for fonts (text reflows once a web font swaps in) before measuring.
                var fonts = (document.fonts && document.fonts.ready)
                    ? document.fonts.ready : Promise.resolve();
                fonts.then(function () {
                    watchImages(function () { if (ready) DogearPager.recompute(); });
                    settle();
                });
            }

            if (document.readyState === 'complete') { start(); }
            else { window.addEventListener('load', start); }

            // Rotation / size change: re-apply column width and re-anchor to the same fraction.
            var rt = null;
            window.addEventListener('resize', function () {
                if (!ready) return;
                var c = pageCount();
                var f = c <= 1 ? 0 : currentPage() / (c - 1);
                if (rt) clearTimeout(rt);
                rt = setTimeout(function () {
                    applyColumnWidth();
                    DogearPager.goToFraction(f);
                    report();
                }, 120);
            });
        })();
    """.trimIndent()
}
