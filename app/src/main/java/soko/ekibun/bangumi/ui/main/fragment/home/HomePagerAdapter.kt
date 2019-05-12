package soko.ekibun.bangumi.ui.main.fragment.home

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen.RakuenFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline.TimeLineFragment

class HomePagerAdapter(private val context: Context, fragmentManager: androidx.fragment.app.FragmentManager, pager: androidx.viewpager.widget.ViewPager): androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {
    val fragments: List<HomeTabFragment> = listOf(
            TimeLineFragment(),
            CollectionFragment(),
            RakuenFragment()
    )

    init{
        pager.offscreenPageLimit = 4
        pager.addOnPageChangeListener(object: androidx.viewpager.widget.ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                fragments.getOrNull(position)?.onSelect()
            } })
    }

    override fun getItem(pos: Int): HomeTabFragment {
        return fragments[pos]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return context.getString(fragments[pos].titleRes)
    }
}