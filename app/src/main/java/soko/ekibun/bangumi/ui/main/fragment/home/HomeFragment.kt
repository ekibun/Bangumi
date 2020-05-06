package soko.ekibun.bangumi.ui.main.fragment.home

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import kotlinx.android.synthetic.main.content_home.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen.RakuenFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline.TimeLineFragment

/**
 * 主页
 * @property titleRes Int
 * @property collectionFragment CollectionFragment
 * @property fragments List<HomeTabFragment>
 * @property checkedPos Int
 */
class HomeFragment: DrawerFragment(R.layout.content_home) {

    private fun <T> findOrCreateFragmentByClassName(clazz: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return (try {
            childFragmentManager.findFragmentByTag(clazz.name)?.also { Log.v("restore", clazz.name) }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            null
        } ?: clazz.newInstance()) as T
    }

    override val titleRes: Int = R.string.home
    val collectionFragment by lazy { findOrCreateFragmentByClassName(CollectionFragment::class.java) }
    private val fragments by lazy {
        mapOf(
            R.id.item_timeline to findOrCreateFragmentByClassName(TimeLineFragment::class.java),
            R.id.item_collect to collectionFragment,
            R.id.item_rakuen to findOrCreateFragmentByClassName(RakuenFragment::class.java)
        )
    }

    private var checkedPos = R.id.item_collect

    /**
     * 选中
     * @param pos Int
     */
    fun select(pos: Int) {
        checkedPos = pos
        val fragment = fragments[checkedPos] ?: return
        childFragmentManager.beginTransaction()
            .replace(R.id.frame_pager, fragment, fragment.javaClass.name).commit()
        activity?.invalidateOptionsMenu()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frame_tabs?.setOnNavigationItemSelectedListener {
            select(it.itemId)
            true
        }
        frame_tabs?.selectedItemId = checkedPos
    }

    override fun processBack(): Boolean {
        if (frame_pager == null || checkedPos == 1) return false
        select(1)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("home_select_index", checkedPos)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt("home_select_index")?.let {
            select(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu) {
        super.onCreateOptionsMenu(menu)
        fragments[checkedPos]?.onCreateOptionsMenu(menu)
    }

    /**
     * 更新用户收藏
     * @return Unit?
     */
    fun updateUserCollection(): Unit? {
        return collectionFragment.reset()
    }

    /**
     * 用户改变
     */
    fun onUserChange() {
        fragments.values.forEach { it.onUserChange() }
    }
}