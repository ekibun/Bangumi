package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.view_login.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 收藏页
 * @property titleRes Int
 * @property iconRes Int
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
        /* no-op */
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        item_pager?.currentItem?.let {
            outState.putInt("collection_fragment_item_index", it)
        }
        (item_pager?.adapter as? CollectionPagerAdapter)?.collectionTypeView?.let {
            outState.putInt("collection_fragment_type_index", it.selectedType)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt("collection_fragment_item_index")?.let {
            item_pager?.currentItem = it
        }
        savedInstanceState?.getInt("collection_fragment_type_index")?.let {
            (item_pager?.adapter as? CollectionPagerAdapter)?.collectionTypeView?.selectedType = it
        }
    }

    override fun onUserChange() {
        val hasUser = UserModel.current() != null
        item_login_info?.visibility = if (!hasUser) View.VISIBLE else View.GONE
        item_pager?.visibility = if (hasUser) View.VISIBLE else View.INVISIBLE
        item_tab_container?.visibility = if (hasUser) View.VISIBLE else View.INVISIBLE
        (item_pager?.adapter as? CollectionPagerAdapter)?.reset()
    }
}