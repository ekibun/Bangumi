package soko.ekibun.bangumi.util.span

import android.graphics.Paint
import android.text.style.ImageSpan

class BaseLineImageSpan(drawable: UrlDrawable, private val source: String? = null) :
    ImageSpan(drawable, ALIGN_BASELINE) {
    override fun getSource(): String? = source
    override fun getDrawable(): UrlDrawable = super.getDrawable() as UrlDrawable
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val ascent = fm?.ascent
        val top = fm?.top
        val descent = fm?.descent
        val bottom = fm?.bottom
        val width = super.getSize(paint, text, start, end, fm)
        fm?.descent = descent
        fm?.bottom = bottom
        fm?.top = Math.min(fm?.top ?: 0, top ?: 0)
        fm?.ascent = Math.min(fm?.ascent ?: 0, ascent ?: 0)
        return width
    }
}