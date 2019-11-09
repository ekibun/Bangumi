package soko.ekibun.bangumi.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_search.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.SwipeBackActivity

class SearchActivity : SwipeBackActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(toolbar)
        supportActionBar?.let{
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        val historyPaddingBottom = search_history.paddingBottom
        val listPaddingBottom = search_list.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            search_history.setPadding(search_history.paddingLeft, search_history.paddingTop, search_history.paddingRight, historyPaddingBottom + insets.systemWindowInsetBottom)
            search_list.setPadding(search_list.paddingLeft, search_list.paddingTop, search_list.paddingRight, listPaddingBottom + insets.systemWindowInsetBottom)
            insets
        }

        SearchPresenter(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object{
        fun startActivity(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}
