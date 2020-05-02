package soko.ekibun.bangumi.ui.view

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.ResourceUtil

class RoundBackgroundDecoration(val offset: Int = 0) : RecyclerView.ItemDecoration() {
    private lateinit var roundCornerDrawable: Drawable
    private val dp24 = ResourceUtil.toPixels(24f)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!::roundCornerDrawable.isInitialized)
            roundCornerDrawable = parent.context.getDrawable(R.drawable.bg_round_dialog)!!
        val layoutManager = (parent.layoutManager as? LinearLayoutManager) ?: return
        val scrollStart = Math.min(layoutManager.findViewByPosition(0)?.let {
            -layoutManager.getDecoratedTop(it) - offset
        } ?: if (layoutManager.findFirstVisibleItemPosition() > 0) dp24 else 0, dp24)

        roundCornerDrawable.setBounds(0, -scrollStart, c.width, c.height)
        roundCornerDrawable.draw(c)
        super.onDraw(c, parent, state)
    }
}