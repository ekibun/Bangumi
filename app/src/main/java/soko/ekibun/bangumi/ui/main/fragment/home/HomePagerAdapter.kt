package soko.ekibun.bangumi.ui.main.fragment.home

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.calendar.CalendarFragment
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.CollectionFragment

class HomePagerAdapter(private val context: Context, fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {
    val collectionFragment = CollectionFragment()
    private val fragments: List<HomeTabFragment> = listOf(
            collectionFragment,
            CalendarFragment()
    )

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