package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.PopupMenu
import kotlinx.android.synthetic.main.fragment_rakuen.*
import kotlinx.android.synthetic.main.item_rakuen_tab.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 超展开
 * @property titleRes Int
 * @property iconRes Int
 * @property refresh Function0<Unit>
 */
class RakuenFragment: HomeTabFragment(R.layout.fragment_rakuen){
    override val titleRes: Int = R.string.rakuen
    override val iconRes: Int = R.drawable.ic_explore

    var refresh = {}
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RakuenPagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)

        val topicTab = LayoutInflater.from(view.context).inflate(R.layout.item_rakuen_tab, item_tabs, false)
        val popup = PopupMenu(view.context, topicTab)
        popup.menuInflater.inflate(R.menu.list_topic_filter, popup.menu)
        popup.setOnMenuItemClickListener{
            if(adapter.selectedFilter != it.itemId) adapter.reset(1)
            adapter.selectedFilter = it.itemId
            topicTab.item_filter.text = it.title
            adapter.loadTopicList()
            true
        }
        topicTab.item_filter.text = popup.menu.findItem(adapter.selectedFilter)?.title
        topicTab.setOnClickListener {
            if(item_pager?.currentItem != 1) item_pager?.currentItem = 1
            else popup.show()
        }

        item_tabs?.getTabAt(1)?.customView = topicTab
        item_new?.setOnClickListener {
            WebActivity.startActivity(view.context, "${Bangumi.SERVER}/rakuen/new_topic")
        }
        refresh = { adapter.loadTopicList() }
    }

    override fun onSelect() {
        refresh()
    }

    override fun onUserChange() {
        // TODO
    }
}