package soko.ekibun.bangumi.ui.main.fragment.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.content_cache.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.util.PlayerBridge

/**
 * 缓存页
 */
class CacheFragment: DrawerFragment(R.layout.content_cache) {
    override val titleRes: Int = R.string.cache

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = (activity as? MainActivity)?:return

        val adapter = CacheAdapter()
        activity.downloadCacheProvider.getCacheList({
            adapter.setNewData(it)
        }, {
            Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
            adapter.setNewData(null) })


        item_list?.layoutManager = LinearLayoutManager(view.context)
        item_list?.adapter = adapter
        item_list?.let{ adapter.emptyView = LayoutInflater.from(view.context).inflate(R.layout.view_empty, it, false) }
        adapter.setOnItemClickListener { _, _, position ->
            PlayerBridge.startActivity(view.context, adapter.data[position].subject.toSubject())
        }
        item_swipe?.setOnRefreshListener {
            activity.downloadCacheProvider.getCacheList({
                adapter.setNewData(it)
                item_swipe?.isRefreshing = false
            }, {
                Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
                item_swipe?.isRefreshing = false
                adapter.setNewData(null) })
        }

        view.setOnApplyWindowInsetsListener { _, insets ->
            item_list?.dispatchApplyWindowInsets(insets)
            insets
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        super.onPrepareOptionsMenu(menu)
    }
}