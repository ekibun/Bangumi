package soko.ekibun.bangumi.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.ViewCompat
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_main.content_frame
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.activity_main.root_layout
import kotlinx.android.synthetic.main.activity_main.toolbar
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarFragment
import soko.ekibun.bangumi.ui.main.fragment.history.HistoryFragment
import soko.ekibun.bangumi.ui.main.fragment.home.HomeFragment
import soko.ekibun.bangumi.ui.main.fragment.index.IndexFragment
import soko.ekibun.bangumi.ui.search.SearchActivity
import soko.ekibun.bangumi.ui.setting.SettingsActivity

/**
 * 抽屉
 * @property context MainActivity
 * @property checkedId Int
 * @property homeFragment HomeFragment
 * @property fragments Map<Int, DrawerFragment>
 * @property toggle ActionBarDrawerToggle
 * @property navigationItemSelectedListener Function1<MenuItem, Boolean>
 * @constructor
 */
class DrawerView(private val context: MainActivity, onLogout: () -> Unit) {
    var checkedId = R.id.nav_home

    private fun <T> findOrCreateFragmentByClassName(clazz: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return (context.supportFragmentManager.findFragmentByTag(clazz.name)?.also {
            Log.v("restore", clazz.name)
        } ?: clazz.newInstance()) as T
    }

    val homeFragment = findOrCreateFragmentByClassName(HomeFragment::class.java)
    val calendarFragment = findOrCreateFragmentByClassName(CalendarFragment::class.java)
    private val fragments: Map<Int, DrawerFragment> = mapOf(
        R.id.nav_home to homeFragment,
        R.id.nav_calendar to calendarFragment,
        R.id.nav_index to findOrCreateFragmentByClassName(IndexFragment::class.java),
        R.id.nav_history to findOrCreateFragmentByClassName(HistoryFragment::class.java)
    )

    val toggle = run {
        context.setSupportActionBar(context.toolbar)
        val toggle = ActionBarDrawerToggle(
            context, context.drawer_layout, context.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        context.drawer_layout.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled =
            context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        toggle.syncState()
        toggle
    }

    var navigationItemSelectedListener = { it: MenuItem ->
        context.drawer_layout.closeDrawers()
        if (fragments.containsKey(it.itemId))
            select(it.itemId)
        else {
            when (it.itemId) {
                R.id.nav_search -> SearchActivity.startActivity(context)
                R.id.nav_setting -> context.startActivity(Intent(context, SettingsActivity::class.java))
                R.id.nav_logout -> onLogout()
            }
        }
        true
    }

    init {
        context.nav_view.setNavigationItemSelectedListener {
            navigationItemSelectedListener(it)
        }
        select(checkedId)
    }

    /**
     * 保存状态
     * @param outState Bundle
     */
    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("DrawerCheckedId", checkedId)
    }

    /**
     * 恢复状态
     * @param savedInstanceState Bundle
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        select(savedInstanceState.getInt("DrawerCheckedId"))
    }

    /**
     * 获取当前fragment
     * @return DrawerFragment?
     */
    fun current(): DrawerFragment? {
        return fragments[checkedId]
    }

    /**
     * 选中fragment
     * @param id Int
     */
    fun select(id: Int) {
        checkedId = id
        val fragment = fragments[id] ?: return
        context.supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment, fragment.javaClass.name).runOnCommit {
                ViewCompat.requestApplyInsets(context.drawer_layout)
            }.commit()
        context.invalidateOptionsMenu()
        context.setTitle(fragment.titleRes)
        context.nav_view.setCheckedItem(id)
    }
}