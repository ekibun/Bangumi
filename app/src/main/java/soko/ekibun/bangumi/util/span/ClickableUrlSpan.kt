package soko.ekibun.bangumi.util.span

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * Url回调Span
 */
class ClickableUrlSpan(
    val url: String,
    var onClick: (View, String) -> Unit = { v, str ->
        WebActivity.launchUrl(v.context, Bangumi.parseUrl(str), "")
    }
) : ClickableSpan() {
    override fun onClick(widget: View) {
        onClick(widget, url)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = false
    }
}