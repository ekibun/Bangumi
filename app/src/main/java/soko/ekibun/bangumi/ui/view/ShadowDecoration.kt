package soko.ekibun.bangumi.ui.view

import android.graphics.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.util.ResourceUtil

class ShadowDecoration(val color: Int, val size: Int) : RecyclerView.ItemDecoration() {

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
        val scrollY = layoutManager.findViewByPosition(0)?.let { -layoutManager.getDecoratedTop(it) }
            ?: if (layoutManager.findFirstVisibleItemPosition() > 0) size else 0
        val gradSize = Math.min(scrollY, size)
        gradientPaint.shader = LinearGradient(0f, 0f, 0f, gradSize.toFloat(), colors, positions, Shader.TileMode.CLAMP)
        c.drawRect(Rect(0, 0, c.width, gradSize), gradientPaint)
    }

    companion object {
        fun set(recyclerView: RecyclerView) {
            recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            recyclerView.addItemDecoration(
                ShadowDecoration(
                    color = ResourceUtil.resolveColorAttr(recyclerView.context, android.R.attr.colorBackground),
                    size = ResourceUtil.toPixels(recyclerView.resources, 32f)
                )
            )
        }
    }
}