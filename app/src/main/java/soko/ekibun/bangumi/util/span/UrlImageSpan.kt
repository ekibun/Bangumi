package soko.ekibun.bangumi.util.span

import android.text.style.ImageSpan

class UrlImageSpan(drawable: UrlDrawable, var url: String = "") : ImageSpan(drawable, "", ALIGN_BASELINE) {

    @Deprecated("use url instead", ReplaceWith("url"))
    override fun getSource(): String? = url
}