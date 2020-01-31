package soko.ekibun.bangumi.ui.view

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.appbar_collapsible_layout.view.*

class CollapsibleAppBarHelper(val appbar: AppBarLayout) {
    var appBarOffset = 0

    init {
        appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            appBarOffset = verticalOffset
            updateStatus()
        })
        appbar.title_collapse.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_TITLE) }
        appbar.title_expand.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_TITLE) }
        appbar.title_slice_0.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_SUBTITLE) }
        appbar.title_slice_1.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_GROUP) }
    }

    private fun updateStatus() {
        val ratio = Math.abs(appBarOffset.toFloat() / appbar.totalScrollRange)
        appbar.title_collapse.alpha = 1 - (1 - ratio) * (1 - ratio) * (1 - ratio)
        appbar.title_expand.alpha = 1 - ratio
        appbar.title_collapse.translationY =
            if (appbar.title_sub.visibility == View.VISIBLE) -appbar.title_sub.height / 2 * ratio else 0f
        appbar.title_expand.translationY = appbar.title_collapse.translationY
        appbar.title_sub.translationY = (appbar.title_collapse.height - appbar.title_expand.height -
                (appbar.title_sub.layoutParams as ConstraintLayout.LayoutParams).topMargin - appbar.title_sub.height / 2) * ratio
    }

    fun setTitle(title: String, subTitle: String? = null, group: String? = null) {
        appbar.title_collapse.text = title
        appbar.title_expand.text = title
        appbar.title_slice_0.text = subTitle
        appbar.title_slice_1.text = group
        appbar.title_sub.visibility = if (subTitle.isNullOrEmpty() && group.isNullOrEmpty()) View.GONE else View.VISIBLE
        appbar.title_slice_divider.visibility = if (group.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        appbar.title_slice_1.visibility = appbar.title_slice_divider.visibility
        updateStatus()
    }

    var onTitleClickListener = { _: ClickEvent -> }

    enum class ClickEvent {
        EVENT_TITLE, EVENT_SUBTITLE, EVENT_GROUP
    }
}