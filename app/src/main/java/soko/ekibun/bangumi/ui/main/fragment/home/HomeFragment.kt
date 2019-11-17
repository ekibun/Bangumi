package soko.ekibun.bangumi.ui.main.fragment.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.content_home.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline.TimeLineFragment

/**
 * 主页
 */
class HomeFragment: DrawerFragment(R.layout.content_home){
    override val titleRes: Int = R.string.home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = HomePagerAdapter(activity!!, childFragmentManager, frame_pager)
        frame_pager.adapter = adapter
        frame_tabs.setupWithViewPager(frame_pager)
        for(i in 0 until frame_tabs.tabCount){
            frame_tabs.getTabAt(i)?.icon =  view.context.getDrawable(adapter.getItem(i).iconRes)
        }
        frame_pager?.currentItem = 1
        changeIconImgBottomMargin(frame_tabs, 0, frame_tabs.tabTextColors)
    }

    private fun changeIconImgBottomMargin(parent: ViewGroup, px: Int, colors: ColorStateList?) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                changeIconImgBottomMargin(child, px, colors)
            } else if (child is ImageView) {
                val lp = child.getLayoutParams() as ViewGroup.MarginLayoutParams
                lp.bottomMargin = 0
                child.imageTintList = colors
                child.requestLayout()
            }
        }
    }

    override fun processBack(): Boolean{
        if(frame_pager == null || frame_pager?.currentItem == 1) return false
        frame_pager?.currentItem = 1
        return true
    }

    /**
     * 收藏fragment
     */
    fun collectionFragment(): CollectionFragment?{
        return fragments()?.firstOrNull { it is CollectionFragment } as? CollectionFragment
    }

    /**
     * 时间线fragment
     */
    fun timelineFragment(): TimeLineFragment?{
        return fragments()?.firstOrNull { it is TimeLineFragment } as? TimeLineFragment
    }

    /**
     * fragment列表
     */
    fun fragments(): List<HomeTabFragment>?{
        return (frame_pager?.adapter as? HomePagerAdapter)?.fragments
    }
}