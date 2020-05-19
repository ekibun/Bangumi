package soko.ekibun.bangumi.util.span

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.TypefaceSpan
import androidx.core.text.toSpanned
import soko.ekibun.bangumi.model.ThemeModel

class CodeLineSpan : TypefaceSpan("monospace"), LeadingMarginSpan, LineBackgroundSpan {
    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        val spanStart = text.toSpanned().getSpanStart(this)
        if (spanStart < 0 || start < spanStart) return
        if (start > 0 && text[start - 1] != '\n') return
        val lineCount = text.substring(spanStart, start).count { it == '\n' } + 1
        val typeFace = p.typeface
        val align = p.textAlign
        val color = p.color
        p.typeface = Typeface.MONOSPACE
        p.textAlign = Paint.Align.RIGHT
        p.color = 0x80808080.toInt()
        c.drawText(lineCount.toString(), x + 40f, baseline.toFloat(), p)
        p.typeface = typeFace
        p.textAlign = align
        p.color = color
    }

    override fun getLeadingMargin(first: Boolean): Int = 50

    override fun drawBackground(
        c: Canvas,
        p: Paint?,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val alpha = ThemeModel.TranslucentPaint.alpha
        ThemeModel.TranslucentPaint.alpha = 10
        c.drawRect(Rect(left, top, right, bottom), ThemeModel.TranslucentPaint)
        ThemeModel.TranslucentPaint.alpha = alpha
    }

}