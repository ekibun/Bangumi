package soko.ekibun.bangumi.ui.view

import android.graphics.Canvas
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.ResourceUtil

class RoundBackgroundDecoration(val offset: Int = 0) : RecyclerView.ItemDecoration() {
    private val roundCornerDrawable by lazy { App.app.getDrawable(R.drawable.bg_round_dialog) }
    private val dp24 = ResourceUtil.toPixels(24f)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = (parent.layoutManager as? LinearLayoutManager) ?: return
        val scrollStart = Math.min(layoutManager.findViewByPosition(0)?.let {
            -layoutManager.getDecoratedTop(it) - offset
        } ?: if (layoutManager.findFirstVisibleItemPosition() > 0) dp24 else 0, dp24)

        roundCornerDrawable.setBounds(0, -scrollStart, c.width, c.height)
        roundCornerDrawable.draw(c)
        super.onDraw(c, parent, state)
    }
}