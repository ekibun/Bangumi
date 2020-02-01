package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ViewFlipper
import soko.ekibun.bangumi.R

/**
 * https://medium.com/@AllanHasegawa/previewing-multiples-item-types-in-a-recyclerview-163aebc2f34a
 */
class HotViewFlipper : ViewFlipper {
    private val initialView: Int

    constructor(context: Context) : super(context) {
        initialView = 0
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val styled = context.theme.obtainStyledAttributes(
            attrs, R.styleable.HotViewFlipperAttrs, 0, 0
        )

        initialView = try {
            styled.getInteger(
                R.styleable.HotViewFlipperAttrs_initialView, 0
            )
        } finally {
            styled.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        displayedChild = initialView % childCount
    }
}