package soko.ekibun.bangumi.ui.main.fragment.home

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen.RakuenFragment

class HomePagerAdapter(private val context: Context, fragmentManager: FragmentManager, pager: ViewPager): FragmentPagerAdapter(fragmentManager) {
    val collectionFragment = CollectionFragment()
    private val fragments: List<HomeTabFragment> = listOf(
            collectionFragment,
            RakuenFragment()
    )

    init{
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
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