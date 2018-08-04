package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_collection.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment

class CollectionFragment: HomeTabFragment(R.layout.fragment_collection){
    override val titleRes: Int = R.string.collect
    override val iconRes: Int = R.drawable.ic_heart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = CollectionPagerAdapter(view.context, this, item_pager)
        item_tabs?.setupWithViewPager(item_pager)

        item_pager?.currentItem = this.savedInstanceState?.getInt("CollectionPage", 2) ?: 2
    }

    fun reset() {
        (item_pager?.adapter as? CollectionPagerAdapter)?.reset()
    }
}