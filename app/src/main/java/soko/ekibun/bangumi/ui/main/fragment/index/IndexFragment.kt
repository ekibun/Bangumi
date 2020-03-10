package soko.ekibun.bangumi.ui.main.fragment.index

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.content_index.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import java.util.*

/**
 * 索引
 * @property titleRes Int
 */
class IndexFragment: DrawerFragment(R.layout.content_index){
    override val titleRes: Int = R.string.index

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = IndexPagerAdapter(this, item_pager)
        item_pager?.adapter = adapter
        item_pager?.currentItem = savedInstanceState?.getInt("index_select_index") ?: getNowIndex()
        item_tabs?.post {
            item_tabs?.setUpWithViewPager(item_pager)
            item_tabs?.post {
                item_tabs?.setCurrentItem(item_pager?.currentItem ?: getNowIndex(), true)
            }
        }

        root_layout?.setOnApplyWindowInsetsListener { _, insets ->
            adapter.windowInsets = insets
            insets
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        item_pager?.currentItem?.let { outState.putInt("index_select_index", it) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt("index_select_index")?.let {
            item_pager?.currentItem = it
        }
    }

    override fun processBack(): Boolean {
        val index = getNowIndex()
        if (item_pager == null || item_pager?.currentItem == index) return false
        item_pager?.currentItem = index
        return true
    }

    private fun getNowIndex(): Int {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        return (year - 1000) * 12 + month
    }
}