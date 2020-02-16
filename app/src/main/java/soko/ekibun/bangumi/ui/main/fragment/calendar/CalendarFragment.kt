package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.os.Bundle
import android.view.Menu
import android.view.View
import kotlinx.android.synthetic.main.content_calendar.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment

/**
 * 时间表
 * @property titleRes Int
 */
class CalendarFragment: DrawerFragment(R.layout.content_calendar) {
    override val titleRes: Int = R.string.calendar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CalendarPagerAdapter(root_layout)
        item_pager?.adapter = adapter
        item_pager?.currentItem = 7
        item_tabs?.post {
            item_tabs?.setUpWithAdapter(CalendarTabAdapter(item_pager))
            item_tabs?.post {
                item_tabs?.setCurrentItem(7, true)
            }
        }

        root_layout?.setOnApplyWindowInsetsListener { _, insets ->
            adapter.windowInsets = insets
            insets
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun processBack(): Boolean {
        if (item_pager == null || item_pager?.currentItem == 7) return false
        item_pager?.currentItem = 7
        return true
    }

    /**
     * 用户收藏改变
     */
    fun onCollectionChange() {
        (activity as? MainActivity)?.mainPresenter?.calendar?.let {
            (item_pager?.adapter as? CalendarPagerAdapter)?.setOnAirList(it)
        }
    }
}