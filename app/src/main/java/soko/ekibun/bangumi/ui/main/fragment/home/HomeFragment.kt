package soko.ekibun.bangumi.ui.main.fragment.home

import android.os.Bundle
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
 */
class HomeFragment: DrawerFragment(R.layout.content_home) {
    override val titleRes: Int = R.string.home
    private val collectionFragment = CollectionFragment()
    private val fragments: List<HomeTabFragment> = listOf(
        TimeLineFragment(),
        collectionFragment,
        RakuenFragment()
    )

    var checkedPos = 0
    fun select(pos: Int) {
        checkedPos = pos
        fragmentManager?.beginTransaction()?.replace(R.id.frame_pager, fragments[checkedPos])?.commit()
        for (i in 0 until (frame_tabs?.childCount ?: 0)) {
            (frame_tabs?.getChildAt(i) as? CheckedTextView)?.isChecked = i == checkedPos
        }
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
        select(1)
    }

    override fun processBack(): Boolean {
        if (frame_pager == null || checkedPos == 1) return false
        select(1)
        return true
    }

    /**
     * 更新用户收藏
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