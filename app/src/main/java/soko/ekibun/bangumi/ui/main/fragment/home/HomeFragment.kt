package soko.ekibun.bangumi.ui.main.fragment.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.content_home.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.util.ResourceUtil
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment


class HomeFragment: DrawerFragment(R.layout.content_home){
    override val titleRes: Int = R.string.home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = HomePagerAdapter(activity!!, childFragmentManager)
        frame_pager.adapter = adapter
        frame_tabs.setupWithViewPager(frame_pager)
        for(i in 0 until frame_tabs.tabCount){
            frame_tabs.getTabAt(i)?.icon =  view.context.getDrawable(adapter.getItem(i).iconRes)
        }
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

    fun collectionFragment(): CollectionFragment?{
        return (frame_pager?.adapter as? HomePagerAdapter)?.collectionFragment
    }
}