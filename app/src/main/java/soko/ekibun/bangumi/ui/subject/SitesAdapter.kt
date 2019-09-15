package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_site.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.github.bean.BangumiItem

class SitesAdapter(data: MutableList<BangumiItem.SitesBean>? = null) :
        BaseQuickAdapter<BangumiItem.SitesBean, BaseViewHolder>(R.layout.item_site, data) {

    override fun convert(helper: BaseViewHolder, item: BangumiItem.SitesBean) {
        helper.itemView.item_site.text = item.site
        helper.itemView.item_site_id.text = if(item.id.isNullOrEmpty()) item.url else item.id
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(item.color)
    }
}