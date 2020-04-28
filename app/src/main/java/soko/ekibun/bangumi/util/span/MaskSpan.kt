package soko.ekibun.bangumi.util.span

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.awarmisland.android.richedittext.view.RichEditText
import soko.ekibun.bangumi.model.ThemeModel
import java.lang.ref.WeakReference

/**
 * 马赛克Span
 */
class MaskSpan : ClickableSpan() {
    var textView: WeakReference<TextView>? = null

    private val edit = textView?.get() is RichEditText
    override fun onClick(widget: View) {
        if (edit) return
        val view = textView?.get() ?: return
        view.tag = if (view.tag == this) null else this
        view.text = view.text
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = ThemeModel.ForegroundPaint.color
        ds.color = if (edit || textView?.get()?.tag == this) ThemeModel.BackgroundPaint.color else Color.TRANSPARENT
    }
}