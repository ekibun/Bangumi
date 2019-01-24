package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_timeline.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment

class TimeLineFragment: HomeTabFragment(R.layout.fragment_timeline){
    override val titleRes: Int = R.string.timeline
    override val iconRes: Int = R.drawable.ic_timelapse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TimeLinePagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)
    }

    override fun onSelect() {
        //TODO
    }
}