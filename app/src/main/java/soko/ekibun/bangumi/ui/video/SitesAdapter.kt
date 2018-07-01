package soko.ekibun.bangumi.ui.video

import android.content.Context
import android.content.res.ColorStateList
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import kotlinx.android.synthetic.main.item_site_menu.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumiData.bean.BangumiItem

class SitesAdapter(context: Context?, data: MutableList<BangumiItem.SitesBean>?) : CommonAdapter<BangumiItem.SitesBean>(context, R.layout.item_site_menu, data) {

    override fun convert(viewHolder: ViewHolder, item: BangumiItem.SitesBean, position: Int) {
        viewHolder.convertView.item_site.text = item.site
        viewHolder.convertView.item_site_id.text = item.id
        viewHolder.convertView.item_site.backgroundTintList = ColorStateList.valueOf(item.color())
    }
}