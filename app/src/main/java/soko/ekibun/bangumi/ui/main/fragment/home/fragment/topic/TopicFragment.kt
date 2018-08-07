package soko.ekibun.bangumi.ui.main.fragment.home.fragment.topic

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_rakuen.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment

class TopicFragment: HomeTabFragment(R.layout.fragment_rakuen){
    override val titleRes: Int = R.string.rakuen
    override val iconRes: Int = R.drawable.ic_explore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = TopicPagerAdapter(view.context, this, item_pager)
        item_tabs?.setupWithViewPager(item_pager)
    }
}