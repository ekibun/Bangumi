package soko.ekibun.bangumi.ui.main.fragment.home

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.content_home.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.util.ResourceUtil
import android.view.ViewGroup
import android.widget.ImageView


class HomeFragment: DrawerFragment(R.layout.content_home){
    override val titleRes: Int = R.string.home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = HomePagerAdapter(activity!!, childFragmentManager)
        frame_pager.adapter = adapter
        frame_tabs.setupWithViewPager(frame_pager)
        for(i in 0 until frame_tabs.tabCount){
            frame_tabs.getTabAt(i)?.icon = ResourceUtil.getTintDrawable(view.context, adapter.getItem(i).iconRes,frame_tabs.tabTextColors)
        }
        changeIconImgBottomMargin(frame_tabs, 0)
    }

    private fun changeIconImgBottomMargin(parent: ViewGroup, px: Int) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                changeIconImgBottomMargin(child, px)
            } else if (child is ImageView) {
                val lp = child.getLayoutParams() as ViewGroup.MarginLayoutParams
                lp.bottomMargin = 0
                child.requestLayout()
            }
        }
    }

    fun resetCollection() {
        (frame_pager?.adapter as? HomePagerAdapter)?.collectionFragment?.reset()
    }
}