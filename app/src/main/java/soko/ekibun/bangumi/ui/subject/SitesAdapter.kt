package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_site.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.github.bean.OnAirInfo

/**
 * 站点Adapter
 */
class SitesAdapter(data: MutableList<OnAirInfo.Site>? = null) :
        BaseQuickAdapter<OnAirInfo.Site, BaseViewHolder>(R.layout.item_site, data) {

    override fun convert(helper: BaseViewHolder, item: OnAirInfo.Site) {
        helper.itemView.item_site.text = item.site
        helper.itemView.item_site_id.text = item.title()
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(item.color)
    }
}