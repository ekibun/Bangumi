package soko.ekibun.bangumi.ui.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.appbar_layout.view.*
import soko.ekibun.bangumi.util.AppUtil

/**
 * App bar helper
 * @property appbar AppBarLayout
 * @property appBarOffset Int
 * @property collapsible CollapseStatus
 * @property mRatio Float
 * @property onTitleClickListener Function1<ClickEvent, Unit>
 * @constructor
 */
class CollapsibleAppBarHelper(val appbar: AppBarLayout) {
    var appBarOffset = 0

    init {
        appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            appBarOffset = verticalOffset
            if (collapsible == CollapseStatus.AUTO) updateStatus(Math.abs(appBarOffset.toFloat() / appbar.totalScrollRange))
        })
        appbar.title_collapse.setOnClickListener {
            if (actionMode != null) hideActionMode() else onTitleClickListener(ClickEvent.EVENT_TITLE)
        }
        appbar.title_expand.setOnClickListener {
            if (actionMode != null) hideActionMode() else onTitleClickListener(ClickEvent.EVENT_TITLE)
        }
        appbar.title_slice_0.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_SUBTITLE) }
        appbar.title_slice_1.setOnClickListener { onTitleClickListener(ClickEvent.EVENT_GROUP) }

        appbar.title_collapse.setOnLongClickListener(::onTitleLongClick)
        appbar.title_expand.setOnLongClickListener(::onTitleLongClick)
    }

    var actionMode: ActionMode? = null
    private fun hideActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    private fun onTitleLongClick(v: View): Boolean {
        return if (Build.VERSION.SDK_INT > 22) {
            actionMode = v.startActionMode(object : ActionMode.Callback2() {
                val ID_COPY = android.R.id.copy
                val ID_SHARE = android.R.id.shareText
                private val MENU_ITEM_ORDER_COPY = 5
                private val MENU_ITEM_ORDER_SHARE = 7

                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.title = null
                    mode.subtitle = null
                    mode.titleOptionalHint = true
                    populateMenuWithItems(menu)
                    return true
                }

                private fun populateMenuWithItems(menu: Menu) {
                    menu.add(
                        Menu.NONE, ID_COPY, MENU_ITEM_ORDER_COPY,
                        "复制"
                    ).setAlphabeticShortcut('c').setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    menu.add(
                        Menu.NONE, ID_SHARE, MENU_ITEM_ORDER_SHARE,
                        "分享"
                    ).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                }

                val clipboardManager by lazy { v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    val str = (v as TextView).text.toString()
                    when (item.itemId) {
                        ID_COPY -> {
                            clipboardManager.primaryClip = ClipData.newPlainText("bangumi_title", str)
                            Toast.makeText(v.context, "已复制到剪贴板", Toast.LENGTH_LONG).show()
                        }
                        ID_SHARE -> AppUtil.shareString(v.context, str)
                    }
                    hideActionMode()
                    return true
                }
            }, ActionMode.TYPE_FLOATING)
            true
        } else false
    }

    enum class CollapseStatus {
        EXPANDED, COLLAPSED, AUTO
    }

    private var collapsible = CollapseStatus.AUTO
    fun appbarCollapsible(status: CollapseStatus) {
        collapsible = status
        //content.nested_scroll.tag = true
        when (status) {
            CollapseStatus.EXPANDED -> {
                appbar.setExpanded(true)
                (appbar.toolbar_layout.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
                updateStatus(0f)
            }
            CollapseStatus.COLLAPSED -> {
                appbar.setExpanded(false)
                (appbar.toolbar_layout.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
                updateStatus(1f)
            }
            CollapseStatus.AUTO -> {
                val params = appbar.toolbar_layout.layoutParams as AppBarLayout.LayoutParams
                params.scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                appbar.toolbar_layout.layoutParams = params
            }
        }
        setTitle(
            appbar.title_collapse.text.toString(),
            appbar.title_slice_0.text.toString(),
            appbar.title_slice_1.text.toString()
        )
    }

    private var mRatio = 0f
    fun updateStatus(ratio: Float = mRatio) {
        hideActionMode()
        mRatio = ratio
        appbar.title_collapse.alpha = 1 - (1 - ratio) * (1 - ratio) * (1 - ratio)
        appbar.title_expand.alpha = 1 - ratio
        appbar.title_collapse.translationY =
            if (appbar.title_sub.visibility == View.VISIBLE) -appbar.title_sub.height / 2 * ratio else 0f
        appbar.title_expand.translationY = appbar.title_collapse.translationY
        appbar.title_sub.translationY = (appbar.title_collapse.height - appbar.title_expand.height -
                (appbar.title_sub.layoutParams as ConstraintLayout.LayoutParams).topMargin - appbar.title_sub.height / 2) * ratio
    }

    fun setTitle(title: String? = null, subTitle: String? = null, group: String? = null) {
        appbar.title_collapse.text = title ?: appbar.title_collapse.text
        appbar.title_expand.text = if (collapsible == CollapseStatus.COLLAPSED) "" else appbar.title_collapse.text
        appbar.title_slice_0.text = subTitle ?: appbar.title_slice_0.text
        appbar.title_slice_1.text = group ?: appbar.title_slice_1.text
        appbar.title_sub.visibility =
            if (appbar.title_slice_0.text.isNullOrEmpty() && appbar.title_slice_1.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        appbar.title_slice_divider.visibility = if (group.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        appbar.title_slice_1.visibility = appbar.title_slice_divider.visibility
        appbar.title_slice_0.requestLayout()
        updateStatus()
    }

    var onTitleClickListener = { _: ClickEvent -> }

    enum class ClickEvent {
        EVENT_TITLE, EVENT_SUBTITLE, EVENT_GROUP
    }
}