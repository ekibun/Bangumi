package soko.ekibun.bangumi.util.span

import android.graphics.Paint
import android.text.style.ImageSpan

class BaseLineImageSpan(drawable: UrlDrawable, private val source: String? = null) :
    ImageSpan(drawable, ALIGN_BASELINE) {
    override fun getSource(): String? = source
    override fun getDrawable(): UrlDrawable = super.getDrawable() as UrlDrawable
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val width = super.getSize(paint, text, start, end, fm)
        fm?.descent = paint.fontMetricsInt.descent
        fm?.bottom = paint.fontMetricsInt.bottom
        fm?.top = Math.min(fm?.top ?: 0, paint.fontMetricsInt.top)
        fm?.ascent = Math.min(fm?.ascent ?: 0, paint.fontMetricsInt.ascent)
        return width
    }
}