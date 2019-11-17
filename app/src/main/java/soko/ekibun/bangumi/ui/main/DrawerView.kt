package soko.ekibun.bangumi.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.switch_item.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.cache.CacheFragment
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarFragment
import soko.ekibun.bangumi.ui.main.fragment.home.HomeFragment
import soko.ekibun.bangumi.ui.main.fragment.index.IndexFragment
import soko.ekibun.bangumi.ui.search.SearchActivity
import soko.ekibun.bangumi.ui.setting.SettingsActivity

/**
 * 抽屉
 */
class DrawerView(private val context: MainActivity, onNightModeChange: (Boolean) -> Unit, onLogout: () -> Unit) {
    var checkedId = R.id.nav_home
    val homeFragment = HomeFragment()
    private val fragments: Map<Int, DrawerFragment> = mapOf(
            R.id.nav_home to homeFragment,
            R.id.nav_calendar to CalendarFragment(),
            R.id.nav_index to IndexFragment(),
            R.id.nav_download to CacheFragment()
    )
    val switch = context.nav_view.menu.findItem(R.id.nav_night).actionView.item_switch!!

    init {
        switch.setOnCheckedChangeListener { _, isChecked ->
            onNightModeChange(isChecked)
        }

        context.content_frame.setOnApplyWindowInsetsListener { _, insets ->
            context.content_frame.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets.replaceSystemWindowInsets(insets.systemWindowInsetLeft, 0, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }

        context.nav_view.setNavigationItemSelectedListener {
            if (it.itemId != R.id.nav_night)
                context.drawer_layout.closeDrawers()
            if (fragments.containsKey(it.itemId))
                select(it.itemId)
            else {
                when (it.itemId) {
                    R.id.nav_search -> SearchActivity.startActivity(context)
                    R.id.nav_setting -> context.startActivity(Intent(context, SettingsActivity::class.java))
                    R.id.nav_night -> switch.isChecked = !switch.isChecked
                    R.id.nav_logout -> onLogout()
                }
            }
            true
        }
        select(checkedId)
    }

    /**
     * 保存状态
     */
    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("DrawerCheckedId", checkedId)
        fragments.forEach {
            it.value.onSaveInstanceState(outState)
        }
    }

    /**
     * 恢复状态
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        select(savedInstanceState.getInt("DrawerCheckedId"))
        fragments.forEach {
            it.value.onRestoreInstanceState(savedInstanceState)
        }
    }

    /**
     * 获取当前fragment
     */
    fun current(): DrawerFragment? {
        return fragments[checkedId]
    }

    /**
     * 选中fragment
     */
    fun select(id: Int) {
        checkedId = id
        context.supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragments[id]!!).commit()
        context.nav_view.setCheckedItem(id)
        context.invalidateOptionsMenu()
        ViewCompat.requestApplyInsets(context.drawer_layout)
    }
}