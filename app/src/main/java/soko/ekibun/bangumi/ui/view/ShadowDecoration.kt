package soko.ekibun.bangumi.ui.view

import android.graphics.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.util.ResourceUtil

class ShadowDecoration(val color: Int, val size: Int, val drawEnd: Boolean) : RecyclerView.ItemDecoration() {

    private val gradientPaint by lazy {
        val paint = Paint()
        paint.isAntiAlias = true
        paint
    }

    private val positions = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    private val colors = floatArrayOf(1f, 0.9f, 0.6f, 0.2f, 0f).map {
        ((255 * it).toInt() shl 24) + (color and 0xffffff)
    }.toIntArray()

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val layoutManager = (parent.layoutManager as? LinearLayoutManager) ?: return
        val isHorizontal = layoutManager.orientation == LinearLayoutManager.HORIZONTAL
        val scrollStart = Math.min(layoutManager.findViewByPosition(0)?.let {
            if (isHorizontal) -layoutManager.getDecoratedLeft(it) else -layoutManager.getDecoratedTop(it)
        } ?: if (layoutManager.findFirstVisibleItemPosition() > 0) size else 0, size)
        gradientPaint.shader = LinearGradient(
            0f, 0f,
            if (isHorizontal) scrollStart.toFloat() else 0f,
            if (isHorizontal) 0f else scrollStart.toFloat(), colors, positions, Shader.TileMode.CLAMP
        )
        c.drawRect(
            Rect(
                0, 0,
                if (isHorizontal) scrollStart else c.width,
                if (isHorizontal) c.height else scrollStart
            ), gradientPaint
        )
        if (!drawEnd) return
        val scrollEnd = layoutManager.findViewByPosition(layoutManager.itemCount - 1)?.let {
            if (isHorizontal) layoutManager.getDecoratedRight(it) - layoutManager.width + layoutManager.paddingRight
            else layoutManager.getDecoratedBottom(it) - layoutManager.height + layoutManager.paddingBottom
        } ?: size
        gradientPaint.shader = LinearGradient(
            c.width.toFloat(),
            c.height.toFloat(),
            if (isHorizontal) c.width.toFloat() - scrollEnd else c.width.toFloat(),
            if (isHorizontal) c.height.toFloat() else c.height.toFloat() - scrollEnd,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        c.drawRect(
            Rect(
                if (isHorizontal) c.width - scrollEnd else 0,
                if (isHorizontal) 0 else c.height - scrollEnd, c.width, c.height
            ), gradientPaint
        )
    }

    companion object {
        fun set(recyclerView: RecyclerView, drawEnd: Boolean = false) {
            recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            recyclerView.addItemDecoration(
                ShadowDecoration(
                    color = ResourceUtil.resolveColorAttr(recyclerView.context, android.R.attr.colorBackground),
                    size = ResourceUtil.toPixels(32f),
                    drawEnd = drawEnd
                )
            )
        }
    }
}