package com.esabook.webviewcode

/**
 *
 */
internal object HtmlCode {
    const val HTML_HEAD = """<html><head><style>
        * {
            font-size: 10px;
        }
        </style>"""

    const val HTML_HEAD_END = "</head><body>"
    const val HTML_BODY_END = "</body></html>"
    const val STYLE_TEXT_WRAP = """"<style>
        " pre {
             white-space: pre-wrap;       /* CSS 3 */
             white-space: -moz-pre-wrap;  /* Mozilla, since 1999 */
             white-space: -pre-wrap;      /* Opera 4-6 */
             white-space: -o-pre-wrap;    /* Opera 7 */
             word-wrap: break-word;       /* Internet Explorer 5.5+ */
         } "
         </style>
"""
    const val HTML_PRE_TEXT_START = """<pre><code class="nohighlight">"""
    const val HTML_PRE_TEXT_END = "</code></pre> \n"
    const val HTML_PRE_CODE_START = "<pre><code class=\""
    const val HTML_PRE_CODE_WRAP_START = """<pre style="white-space: pre-wrap;"><code class=""""
    const val HTML_PRE_CODE_CLASS = "\">"
    const val HTML_PRE_CODE_END = "</code></pre> \n"
}
