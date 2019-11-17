package soko.ekibun.bangumi.ui.view

import com.chad.library.adapter.base.loadmore.LoadMoreView
import soko.ekibun.bangumi.R

/**
 * 加载更多View
 */
class BrvahLoadMoreView : LoadMoreView() {
    override fun getLayoutId(): Int {
        return R.layout.brvah_quick_view_load_more
    }

    override fun getLoadingViewId(): Int {
        return R.id.load_more_loading_view
    }

    override fun getLoadEndViewId(): Int {
        return R.id.load_more_load_end_view
    }

    override fun getLoadFailViewId(): Int {
        return R.id.load_more_load_fail_view
    }
}