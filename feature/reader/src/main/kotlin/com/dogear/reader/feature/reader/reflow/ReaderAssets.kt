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
 * Pagination technique (CSS multi-column, matching epub.js `src/contents.js` and foliate-js
 * `paginator.js`):
 *  - The **body** is both the multi-column box *and* the horizontal scroll container.
 *  - **The column box is given an explicit pixel height from `window.innerHeight` in JS**, NOT
 *    `height:100%`/`100vh`. This is the crux: Chrome/WebKit only honor `column-fill:auto` when the
 *    container has a *definite* block-size; a percentage/vh height does not resolve to a definite
 *    height inside a WebView, so the engine balances into a single column and the text collapses to
 *    one line that overflows sideways. Forcing pixels fixes that.
 *    (MDN column-fill; CSSWG#4689; epub.js/foliate-js both set `style.height = <px>`.)
 *  - Horizontal reading margin lives in `margin` (not padding) and `column-gap` is 0, so the column
 *    pitch == `body.clientWidth` exactly — no `innerWidth`/padding drift. We page by `body.scrollLeft`
 *    (the window does not scroll when body is `overflow:hidden`).
 *  - Measurement is deferred until `window.load` + `document.fonts.ready` + a `requestAnimationFrame`
 *    stability poll (fonts/images reflow text and change `scrollWidth`); only then is the saved
 *    position restored. Re-pagination is driven by a `ResizeObserver` for rotation/size changes.
 */
internal object ReaderAssets {

    /** width=device-width + scale 1 so CSS px == device px (needs `useWideViewPort=true`). */
    private const val VIEWPORT_META =
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, " +
            "maximum-scale=1.0, user-scalable=no, viewport-fit=cover\">"

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
        val boot = "window.__dogearSmooth=$smoothPaging;" +
            "window.__dogearPending=$pendingFraction;" +
            "window.__dogearHMargin=${style.horizontalMarginPx};"
        val scriptTag = "<script id=\"dogear-pager\">$boot\n$PAGINATION_JS</script>"
        val headInjection = "$VIEWPORT_META$styleTag"

        var html = bodyHtml
        html = if (html.contains("</head>", ignoreCase = true)) {
            html.replaceFirst(Regex("(?i)</head>"), "$headInjection</head>")
        } else {
            "$headInjection$html"
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
            margin: 0 !important;
            padding: 0 !important;
            -webkit-text-size-adjust: 100%;
            background: ${s.backgroundColor};
        }
        body {
            box-sizing: border-box !important;
            /* width/height are overwritten in JS with explicit px; these are only fallbacks.
               Horizontal reading margin is `margin` so it stays OUT of the column pitch; vertical
               margin is padding (does not affect horizontal geometry). Geometry is forced with
               !important so author stylesheets cannot break pagination. */
            width: auto !important;
            height: 100vh;
            margin: 0 ${s.horizontalMarginPx}px !important;
            padding: ${s.verticalMarginPx}px 0 !important;
            column-width: 100% !important;
            -webkit-column-width: 100% !important;
            column-count: auto !important;
            -webkit-column-count: auto !important;
            column-gap: 0 !important;
            -webkit-column-gap: 0 !important;
            column-fill: auto !important;
            -webkit-column-fill: auto !important;
            overflow: hidden !important;
            /* Stop glyph ascenders/descenders being clipped at column edges (epub.js #983). */
            -webkit-line-box-contain: block glyphs replaced;
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
     * scroll container. Geometry is derived from the body's own `clientWidth`/`scrollWidth`/
     * `scrollLeft`; the body height is forced to `window.innerHeight` in px so `column-fill:auto`
     * fragments the text into page-height columns instead of collapsing to one line.
     */
    private val PAGINATION_JS = """
        (function () {
            var el = document.body;
            var ready = false;

            function gutter() { return window.__dogearHMargin || 0; }
            function viewportW() {
                return Math.max(1, document.documentElement.clientWidth || window.innerWidth || 1);
            }
            function viewportH() {
                return Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1);
            }
            // The single column is exactly the viewport minus the two reading gutters.
            function pitch() { return Math.max(1, viewportW() - 2 * gutter()); }
            function applySize() {
                // ALL geometry is forced as inline !important so it beats any author stylesheet
                // rule regardless of selector specificity (this is what foliate-js / epub.js do).
                // Pinning width AND column-width to the SAME value guarantees exactly one column
                // fits the content box — never the two-abutting-columns artifact. The explicit px
                // height is what makes column-fill:auto fragment into page-height columns (Chrome
                // only honors column-fill with a definite block-size).
                var w = pitch() + 'px';
                var p = el.style;
                p.setProperty('width', w, 'important');
                p.setProperty('height', viewportH() + 'px', 'important');
                p.setProperty('column-width', w, 'important');
                p.setProperty('-webkit-column-width', w, 'important');
                p.setProperty('column-count', 'auto', 'important');
                p.setProperty('-webkit-column-count', 'auto', 'important');
                p.setProperty('column-gap', '0px', 'important');
                p.setProperty('-webkit-column-gap', '0px', 'important');
                p.setProperty('column-fill', 'auto', 'important');
                p.setProperty('-webkit-column-fill', 'auto', 'important');
                p.setProperty('overflow', 'hidden', 'important');
                p.setProperty('padding-left', '0px', 'important');
                p.setProperty('padding-right', '0px', 'important');
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
                recompute: function () { applySize(); report(); }
            };

            // Re-measure when late images finish (they shift text and change scrollWidth).
            function watchImages() {
                var imgs = el.querySelectorAll('img');
                for (var i = 0; i < imgs.length; i++) {
                    var img = imgs[i];
                    if (!img.complete) {
                        img.addEventListener('load', DogearPager.recompute, { once: true });
                        img.addEventListener('error', DogearPager.recompute, { once: true });
                    }
                }
            }

            // Poll until scrollWidth stops changing (fonts/images settling), then settle once.
            function whenStable(done) {
                var last = -1, tries = 0;
                function tick() {
                    applySize();
                    var w = el.scrollWidth;
                    if (w === last || tries > 20) { done(); return; }
                    last = w;
                    tries++;
                    requestAnimationFrame(tick);
                }
                requestAnimationFrame(tick);
            }

            function settle() {
                applySize();
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
                    watchImages();
                    settle();
                });
            }

            if (document.readyState === 'complete') { start(); }
            else { window.addEventListener('load', start); }

            // Rotation / size change: re-apply size and re-anchor to the same fraction.
            var rt = null;
            function onResize() {
                if (!ready) return;
                var c = pageCount();
                var f = c <= 1 ? 0 : currentPage() / (c - 1);
                if (rt) clearTimeout(rt);
                rt = setTimeout(function () {
                    applySize();
                    DogearPager.goToFraction(f);
                    report();
                }, 120);
            }
            if (window.ResizeObserver) {
                try { new ResizeObserver(onResize).observe(document.documentElement); } catch (e) {}
            }
            window.addEventListener('resize', onResize);
        })();
    """.trimIndent()
}
