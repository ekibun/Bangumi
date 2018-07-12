package soko.ekibun.bangumi.ui.main

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.switch_item.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarFragment
import soko.ekibun.bangumi.ui.main.fragment.collection.CollectionFragment
import soko.ekibun.bangumi.ui.main.fragment.index.IndexFragment
import soko.ekibun.bangumi.ui.search.SearchActivity

class DrawerView(private val context: MainActivity, onNightModeChange: (Boolean)->Unit, onLogout: ()->Unit){
    private var checkedId = R.id.nav_chase
    private val fragments: Map<Int, DrawerFragment> = mapOf(
            R.id.nav_chase to CollectionFragment(),
            R.id.nav_calendar to CalendarFragment(),
            R.id.nav_index to IndexFragment()
    )
    val switch = context.nav_view.menu.findItem(R.id.nav_night).actionView.item_switch!!

    init{
        context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        context.window.statusBarColor = Color.TRANSPARENT

        context.setSupportActionBar(context.toolbar)
        val toggle = ActionBarDrawerToggle(context, context.drawer_layout, context.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        context.drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        switch.setOnCheckedChangeListener { _, isChecked ->
            onNightModeChange(isChecked)
        }

        context.nav_view.setNavigationItemSelectedListener {
            if(it.itemId != R.id.nav_night)
                context.drawer_layout.closeDrawers()
            if(fragments.containsKey(it.itemId))
                select(it.itemId)
            else{
                when(it.itemId){
                    R.id.nav_search -> SearchActivity.startActivity(context)
                    R.id.nav_night -> switch.isChecked = !switch.isChecked
                    R.id.nav_logout -> onLogout()
                    //R.id.nav_setting -> {}//SettingsActivity.startActivity(context)
                }
            }
            true }
        select(checkedId)
    }

    fun setUser(user: UserInfo?){
        (fragments[R.id.nav_chase] as? CollectionFragment)?.user = user
    }

    fun resetCollection(){
        (fragments[R.id.nav_chase] as? CollectionFragment)?.reset()
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        fragments[checkedId]?.onPrepareOptionsMenu(menu)
    }

    fun onSaveInstanceState(outState: Bundle){
        outState.putInt("DrawerCheckedId", checkedId)
        fragments.forEach {
            it.value.onSaveInstanceState(outState)
        }
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle){
        select(savedInstanceState.getInt("DrawerCheckedId"))
        fragments.forEach {
            it.value.onRestoreInstanceState(savedInstanceState)
        }
    }

    private fun select(id: Int){
        checkedId = id
        context.supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragments[id]).commit()
        context.nav_view.setCheckedItem(id)
        context.invalidateOptionsMenu()
    }
}