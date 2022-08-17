package com.esabook.webviewcode

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.webkit.WebView
import com.esabook.webviewcode.Settings.Lang
import com.esabook.webviewcode.Settings.WithStyle
import java.util.*

/**
 *
 */
public class Codeview {
    private var isTextWrap = false
    private var style: String
    private var language: String

    /**
     * @param code the code will be highlight
     * @return
     */
    public fun withCode(code: SpannableStringBuilder): Codeview {
        appender(Content(code, Content.TYPE_CODE))
        return this
    }

    /**
     * @param text will be treated as simple text no highlight
     * @return
     */
    public fun withText(text: SpannableStringBuilder): Codeview {
        appender(Content(text, Content.TYPE_TEXT))
        return this
    }

    /**
     * @param html will add html code right after ,
     * can call multiple times to append extra html code into body section
     * @return
     */
    public fun withHtml(html: SpannableStringBuilder): Codeview {
        appender(Content(html, Content.TYPE_HTML))
        return this
    }

    /**
     * @param htmlHeadContent set link,css,javascript tags inside
     * @return
     */
    public fun setHtmlHeadContent(htmlHeadContent: String): Codeview {
        htmlWrapper[1] += "$htmlHeadContent \n"
        return this
    }

    /**
     * @param style set the highlight style for the code
     * if style is null or empty Default style will be applied.
     * @return
     * @see com.esabook.webviewcode.Settings.WithStyle
     */
    public fun setStyle(style: String?): Codeview {
        if (style != null && !style.isEmpty()) {
            this.style = style
        } else {
            this.style = DEFAULT_STYLE
        }
        return this
    }

    public fun setLang(lang: String?): Codeview {
        if (lang != null && !lang.isEmpty()) {
            language = lang
        } else {
            language = DEFAULT_LANG
        }
        return this
    }

    public fun setAutoWrap(wrap: Boolean): Codeview {
        isTextWrap = wrap
        return this
    }

    /**
     * @param webview or any subclass of webview
     * @param <T>
    </T> */
    @SuppressLint("SetJavaScriptEnabled")
    public fun <T : WebView?> into(webview: T?) {
        requireNotNull(webview) { "webview cannot be null" }

        //checkWebview(webview);
        if (!webview.settings.javaScriptEnabled) {
            webview.settings.javaScriptEnabled = true
            webview.settings.builtInZoomControls = true
            webview.settings.displayZoomControls = false
            webview.settings.setSupportZoom(true)
            webview.isVerticalScrollBarEnabled = false
            webview.isHorizontalScrollBarEnabled = false
        }

        val htmlDocument = Builder.makeDocument(
            htmlWrapper, Options(isTextWrap, style, language, contents)
        )
        webview.loadDataWithBaseURL(null, htmlDocument, "text/html", "utf-8", "")
    }

    private fun appender(con: Content) {
        contents.add(con)
    }

    /**
     * builder class...
     */
    private class Builder(context: Context?) {
        private val context: Context

        /**
         * @return
         */
        fun build(): Codeview {
            return Codeview()
        }

        companion object {
            /**
             * @param content the array string containing the code,text and headers
             * @param options the users options
             * @return the complete html document
             */
            fun makeDocument(content: Array<String?>, options: Options): String {
                content[0] = "${HtmlCode.HTML_HEAD}\n"
                content[1] = "${content[1].toString()}<style>${options.style}</style>\n"
                content[1] = "${content[1].toString()}<script>${HighlightLib.HIGHLIGHTJS}</script>\n"
                if (options.isTextWrap) {
                    content[1] = "${content[1].toString()}<style>${HtmlCode.STYLE_TEXT_WRAP}</style>\n"
                }
                content[1] = " ${content[1].toString()}<script>hljs.highlightAll();</script>\n"
                content[2] = "${HtmlCode.HTML_HEAD_END}\n"
                content[3] += extractBuildContent(
                    options.contents,
                    options.isTextWrap,
                    options.lang
                )
                content[4] = "${HtmlCode.HTML_BODY_END}\n"
                return content[0] + content[1] + content[2] + content[3] + content[4]
            }
        }


        init {
            requireNotNull(context) { "Context can't be null" }
            this.context = context.applicationContext
        }
    }

    private class Options(
        val isTextWrap: Boolean,
        val style: String,
        val lang: String,
        val contents: List<Content>
    )

    private fun <T : WebView?> checkWebview(webview: T) {
//        if(Build.VERSION.SDK_INT < 18) {
//            webview.clearView();
//
//        } else {
        webview!!.loadDataWithBaseURL(null, "about:blank", "text/html", "utf-8", "")
        //        }
    }

    public companion object {
        public const val DEFAULT_STYLE: String = WithStyle.GITHUB
        public const val DEFAULT_LANG: String = Lang.JSON
        private var htmlWrapper: Array<String?> = arrayOf()
        private var contents: ArrayList<Content> = arrayListOf()

        /**
         * @param contents
         * @return
         */
        private fun extractBuildContent(
            contents: List<Content>,
            isTextWrap: Boolean,
            language: String
        ): String? {
            if (!contents.isEmpty()) {
                val content = StringBuilder()
                for (i in contents.indices) {
                    if (contents[i].type == Content.TYPE_CODE) {
                        if (isTextWrap) {
                            content.append(HtmlCode.HTML_PRE_CODE_WRAP_START)
                                .append(language)
                                .append(HtmlCode.HTML_PRE_CODE_CLASS)
                                .append(contents[i].text)
                                .append(HtmlCode.HTML_PRE_CODE_END)
                        } else {
                            content.append(HtmlCode.HTML_PRE_CODE_START)
                                .append(language)
                                .append(HtmlCode.HTML_PRE_CODE_CLASS)
                                .append(contents[i].text)
                                .append(HtmlCode.HTML_PRE_CODE_END)
                        }
                    } else if (contents[i].type == Content.TYPE_HTML) {
                        content.append(contents[i].text)
                    } else {
                        content.append(HtmlCode.HTML_PRE_TEXT_START)
                            .append(contents[i].text)
                            .append(HtmlCode.HTML_PRE_TEXT_END)
                    }
                }
                return content.toString()
            }
            return null
        }

        private fun resetCache() {
            contents = ArrayList()
            htmlWrapper = arrayOf("", "", "", "", "")
        }

        /**
         * @return a list<String> with all supported styles
         * @see com.esabook.webviewcode.Settings.WithStyle
        </String> */
        public val allCodeStyles: List<String>
            get() {
                val fields = WithStyle::class.java.fields
                val styles: MutableList<String> = ArrayList()
                for (f in fields) {
                    styles.add(f.name)
                }
                return styles
            }

        /**
         * @return
         */
        public val allLanguages: List<String>
            get() {
                val fields = Lang::class.java.fields
                val langs: MutableList<String> = ArrayList()
                for (f in fields) {
                    langs.add(f.name)
                }
                return langs
            }
    }

    init {
        htmlWrapper = arrayOf("", "", "", "", "")
        contents = ArrayList()
        language = DEFAULT_LANG
        style = DEFAULT_STYLE
    }
}
