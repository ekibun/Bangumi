package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_site.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.github.bean.OnAirInfo

/**
 * 站点Adapter
 * @property collapseLabel Boolean
 * @constructor
 */
class SitesAdapter(private val collapseLabel: Boolean = false, data: MutableList<OnAirInfo.Site>? = null) :
        BaseQuickAdapter<OnAirInfo.Site, BaseViewHolder>(R.layout.item_site, data) {

    override fun convert(holder: BaseViewHolder, item: OnAirInfo.Site) {
        holder.itemView.item_site.text = item.site
        holder.itemView.item_site_id.text = item.title()
        holder.itemView.item_site.backgroundTintList = ColorStateList.valueOf(item.color)
        holder.itemView.item_site_id.visibility =
            if (collapseLabel && getItemOrNull(holder.adapterPosition + 1)?.title() == item.title())
                View.GONE else View.VISIBLE
    }
}