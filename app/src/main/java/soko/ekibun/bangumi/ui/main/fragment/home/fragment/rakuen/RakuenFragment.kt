package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_rakuen.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.web.WebActivity

class RakuenFragment: HomeTabFragment(R.layout.fragment_rakuen){
    override val titleRes: Int = R.string.rakuen
    override val iconRes: Int = R.drawable.ic_explore

    var refresh = {}
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RakuenPagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)
        item_new?.setOnClickListener {
            WebActivity.launchUrl(view.context, "${Bangumi.SERVER}/rakuen/new_topic")
        }
        refresh = { adapter.loadTopicList() }
    }

    override fun onSelect() {
        refresh()
    }
}