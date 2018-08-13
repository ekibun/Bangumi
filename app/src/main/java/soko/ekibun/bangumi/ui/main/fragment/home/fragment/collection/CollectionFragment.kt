package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.view_login.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.web.WebActivity

class CollectionFragment: HomeTabFragment(R.layout.fragment_collection){
    override val titleRes: Int = R.string.collect
    override val iconRes: Int = R.drawable.ic_heart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = CollectionPagerAdapter(view.context, this, item_pager)
        item_tabs?.setupWithViewPager(item_pager)

        item_pager?.currentItem = this.savedInstanceState?.getInt("CollectionPage", 2) ?: 2

        item_login_info?.visibility = if(UserModel(item_login.context).getToken() == null) View.VISIBLE else View.GONE
        item_login?.setOnClickListener {
            WebActivity.startActivityForAuth(activity!!) }
    }

    fun reset() {
        (item_pager?.adapter as? CollectionPagerAdapter)?.reset()
        item_login_info?.visibility = if(UserModel(item_login.context).getToken() == null) View.VISIBLE else View.GONE
    }

    override fun onSelect() {
        //TODO
    }
}