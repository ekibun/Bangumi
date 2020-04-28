package soko.ekibun.bangumi.util.span

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import soko.ekibun.bangumi.model.ThemeModel

class QuoteLineSpan : LeadingMarginSpan, LineBackgroundSpan {
    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        c.drawRect(Rect(left, top, right, bottom), ThemeModel.TranslucentPaint)
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        c.drawRect(Rect(x, top, x + 10, bottom), ThemeModel.TranslucentPaint)
    }

    override fun getLeadingMargin(first: Boolean): Int = 20

}