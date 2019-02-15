package soko.ekibun.bangumi.ui.main.fragment.cache

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import kotlinx.android.synthetic.main.content_cache.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.util.PlayerBridge

class CacheFragment: DrawerFragment(R.layout.content_cache) {
    override val titleRes: Int = R.string.cache

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CacheAdapter(PlayerBridge.getVideoCacheList(view.context))
        item_list?.layoutManager = LinearLayoutManager(view.context)
        item_list?.adapter = adapter
        item_list?.let{ adapter.emptyView = LayoutInflater.from(view.context).inflate(R.layout.view_empty, it, false) }
        adapter.setOnItemClickListener { _, _, position ->
            PlayerBridge.startActivity(view.context, adapter.data[position].bangumi)
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        super.onPrepareOptionsMenu(menu)
    }
}