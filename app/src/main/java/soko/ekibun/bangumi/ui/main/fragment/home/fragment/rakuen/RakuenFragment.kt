package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_rakuen.*
import kotlinx.android.synthetic.main.item_rakuen_tab.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.topic.EmojiAdapter
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 超展开
 * @property titleRes Int
 * @property iconRes Int
 * @property refresh Function0<Unit>
 */
class RakuenFragment: HomeTabFragment(R.layout.fragment_rakuen){
    override val titleRes: Int = R.string.rakuen
    override val iconRes: Int = R.drawable.ic_explore

    var selectedFilter = R.id.topic_filter_all

    var refresh = {}
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RakuenPagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)

        val topicTab = LayoutInflater.from(view.context).inflate(R.layout.item_rakuen_tab, item_tabs, false)
        val popup = PopupMenu(view.context, topicTab)
        popup.menuInflater.inflate(R.menu.list_topic_filter, popup.menu)
        popup.setOnMenuItemClickListener {
            if (selectedFilter != it.itemId) adapter.reset(1)
            selectedFilter = it.itemId
            topicTab.item_filter.text = it.title
            adapter.loadTopicList()
            true
        }
        topicTab.item_filter.text = popup.menu.findItem(selectedFilter)?.title
        topicTab.setOnClickListener {
            if (item_pager?.currentItem != 1) item_pager?.currentItem = 1
            else {
                ResourceUtil.checkMenu(view.context, popup.menu) {
                    selectedFilter == it.itemId
                }
                popup.show()
            }
        }

        item_tabs?.getTabAt(1)?.customView = topicTab
        refresh = { adapter.loadTopicList() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        item_pager?.currentItem?.let {
            outState.putInt("rakuen_fragment_item_index", it)
        }
        outState.putInt("rakuen_fragment_filter_index", selectedFilter)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt("rakuen_fragment_item_index")?.let {
            item_pager?.currentItem = it
        }
        savedInstanceState?.getInt("rakuen_fragment_filter_index")?.let {
            selectedFilter = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu) {
        super.onCreateOptionsMenu(menu)
        menu.add("添加").setIcon(R.drawable.ic_add).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            .setOnMenuItemClickListener {
                context?.let { WebActivity.startActivity(it, "${Bangumi.SERVER}/rakuen/new_topic") }
                true
            }
    }

    override fun onSelect() {
        refresh()
    }

    override fun onUserChange() {
        // TODO
    }
}