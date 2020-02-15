package soko.ekibun.bangumi.ui.search

import android.content.Context
import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.fragment_search.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.BaseFragmentActivity

/**
 * 搜索Activity
 */
class SearchActivity : BaseFragmentActivity(R.layout.fragment_search) {

    override fun onViewCreated(view: View) {
        val historyPaddingBottom = search_history.paddingBottom
        val listPaddingBottom = search_list.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            search_history.setPadding(
                search_history.paddingLeft,
                search_history.paddingTop,
                search_history.paddingRight,
                historyPaddingBottom + insets.systemWindowInsetBottom
            )
            search_list.setPadding(
                search_list.paddingLeft,
                search_list.paddingTop,
                search_list.paddingRight,
                listPaddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }

        SearchPresenter(this)
    }

    companion object{
        /**
         * 启动Activity
         * @param context Context
         */
        fun startActivity(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}
