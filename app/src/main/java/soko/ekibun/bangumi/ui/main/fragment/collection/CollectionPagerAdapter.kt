package soko.ekibun.bangumi.ui.main.fragment.collection

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import soko.ekibun.bangumi.R

class CollectionPagerAdapter(context: Context, val fragment: CollectionFragment) : PagerAdapter(){
    private val tabList = context.resources.getStringArray(R.array.collection_status)!!

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val v: View = fragment.viewList[position]
        container.addView(v)
        if (v.tag == null) { fragment.loadCollectionList(position) }
        return v
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return tabList.size
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return tabList[pos]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}