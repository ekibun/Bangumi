package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.view_login.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 收藏页
 */
class CollectionFragment: HomeTabFragment(R.layout.fragment_collection){
    override val titleRes: Int = R.string.collect
    override val iconRes: Int = R.drawable.ic_heart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = CollectionPagerAdapter(view.context, this, item_pager)
        item_tabs?.setupWithViewPager(item_pager)

        item_login?.setOnClickListener {
            WebActivity.startActivityForAuth(activity!!)
        }

        item_pager?.post {
            reset()
        }
    }

    /**
     * 重置
     */
    fun reset() {
        (item_pager?.adapter as? CollectionPagerAdapter)?.reset()
    }

    override fun onSelect() {
        item_tab_container?.isPressed = (item_pager?.adapter as? CollectionPagerAdapter)?.isScrollDown ?: false
    }

    override fun onUserChange() {
        val hasUser = (activity as? MainActivity)?.user != null
        item_login_info?.visibility = if (!hasUser) View.VISIBLE else View.GONE
        item_pager?.visibility = if (hasUser) View.VISIBLE else View.INVISIBLE
        item_tab_container?.visibility = if (hasUser) View.VISIBLE else View.INVISIBLE
    }
}