package soko.ekibun.bangumi.ui.main.fragment.home

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.CheckedTextView
import kotlinx.android.synthetic.main.content_home.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
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
    private val collectionFragment by lazy { findOrCreateFragmentByClassName(CollectionFragment::class.java) }
    private val fragments: List<HomeTabFragment> by lazy {
        listOf(
            findOrCreateFragmentByClassName(TimeLineFragment::class.java),
            collectionFragment,
            findOrCreateFragmentByClassName(RakuenFragment::class.java)
        )
    }

    private var checkedPos = 1

    /**
     * 选中
     * @param pos Int
     */
    fun select(pos: Int) {
        checkedPos = pos
        childFragmentManager.beginTransaction()
            .replace(R.id.frame_pager, fragments[checkedPos], fragments[checkedPos].javaClass.name).commit()
        for (i in 0 until (frame_tabs?.childCount ?: 0)) {
            (frame_tabs?.getChildAt(i) as? CheckedTextView)?.isChecked = i == checkedPos
        }
        activity?.invalidateOptionsMenu()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 0 until (frame_tabs?.childCount ?: 0)) {
            (frame_tabs?.getChildAt(i) as? CheckedTextView)?.let { tv ->
                tv.setOnClickListener {
                    select(i)
                }
                tv.compoundDrawables.forEach {
                    it?.setTintList(tv.textColors)
                }
            }
        }
        select(checkedPos)
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
        fragments[checkedPos].onCreateOptionsMenu(menu)
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
        fragments.forEach { it.onUserChange() }
    }
}