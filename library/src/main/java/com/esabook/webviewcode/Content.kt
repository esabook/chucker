package com.esabook.webviewcode

import android.text.SpannableStringBuilder

/**
 *
 */
public class Content(public val text: SpannableStringBuilder, public val type: String) {

    public companion object {
        public const val TYPE_TEXT: String = "text"
        public const val TYPE_CODE: String = "code"
        public const val TYPE_HTML: String = "html"
    }
}
