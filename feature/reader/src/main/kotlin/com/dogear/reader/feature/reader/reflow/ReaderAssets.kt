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
 * Pagination technique (from the Reader Engineering Research): paginate with CSS columns sized
 * to the viewport, then page by horizontal `window.scrollTo`. Page count is computed by
 * **rounding** `scrollWidth / innerWidth` (avoids the blank/last-page artifact), and images and
 * un-splittable blocks are constrained so they never overflow or split across a page edge.
 */
internal object ReaderAssets {

    fun buildHtml(bodyHtml: String, style: ReflowStyle, smoothPaging: Boolean = false): String {
        val css = css(style)
        val styleTag = "<style id=\"dogear-style\">$css</style>"
        val scriptTag =
            "<script id=\"dogear-pager\">window.__dogearSmooth=$smoothPaging;\n$PAGINATION_JS</script>"

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
        html { height: 100%; -webkit-text-size-adjust: 100%; }
        body {
            margin: 0;
            height: 100vh;
            box-sizing: border-box;
            padding: ${s.verticalMarginPx}px ${s.horizontalMarginPx}px;
            column-width: 100vw;
            column-gap: 0;
            column-fill: auto;
            -webkit-column-width: 100vw;
            overflow: hidden;
            font-family: ${s.fontFamilyCss};
            font-size: ${s.fontSizePx}px;
            font-weight: ${s.fontWeight};
            line-height: ${s.lineHeight};
            color: ${s.textColor};
            background: ${s.backgroundColor};
            text-align: ${s.textAlign};
            word-wrap: break-word;
            -webkit-hyphens: ${if (s.hyphens) "auto" else "none"};
            hyphens: ${if (s.hyphens) "auto" else "none"};
        }
        img, svg, video {
            max-width: 100% !important;
            max-height: 92vh !important;
            height: auto;
            break-inside: avoid;
            -webkit-column-break-inside: avoid;
        }
        table, pre, figure, blockquote, h1, h2, h3 {
            break-inside: avoid;
            -webkit-column-break-inside: avoid;
        }
        a { color: ${s.linkColor}; text-decoration: none; }
        p { margin: 0 0 ${s.paragraphSpacingEm}em 0; }
    """.trimIndent()

    /**
     * Page measurement and navigation. Exposes `DogearPager` to the host and reports page counts
     * back through the `DogearBridge` interface after every layout/move.
     */
    private val PAGINATION_JS = """
        (function () {
            function step() { return window.innerWidth; }
            function pageCount() {
                return Math.max(1, Math.round(document.body.scrollWidth / step()));
            }
            function currentPage() { return Math.round(window.scrollX / step()); }
            function report() {
                if (window.DogearBridge) {
                    DogearBridge.onPaginated(pageCount(), currentPage());
                }
            }
            function goTo(p) {
                var c = pageCount();
                p = Math.max(0, Math.min(c - 1, p));
                window.scrollTo({ left: p * step(), top: 0, behavior: window.__dogearSmooth ? 'smooth' : 'auto' });
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
                    goTo(Math.round(f * (c - 1)));
                },
                recompute: report
            };
            function init() { report(); }
            if (document.readyState === 'complete') { init(); }
            else { window.addEventListener('load', init); }
            window.addEventListener('resize', report);
        })();
    """.trimIndent()
}
