package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_season.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 季度列表Adapter
 * @property currentId Int
 * @constructor
 */
class SeasonAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>
        (R.layout.item_season, data) {
    var currentId = 0

    override fun convert(holder: BaseViewHolder, item: Subject) {
        holder.setText(R.id.item_desc, item.displayName)
        holder.itemView.item_desc.setBackgroundResource(
            when (getItemPosition(item)) {
                0 -> R.drawable.bangumi_detail_ic_season_first
                data.size - 1 -> R.drawable.bangumi_detail_ic_season_last
                else -> R.drawable.bangumi_detail_ic_season_middle
            }
        )
        val color = ResourceUtil.resolveColorAttr(
            holder.itemView.context,
            if (currentId == item.id) R.attr.colorPrimary
            else android.R.attr.textColorSecondary
        )
        holder.itemView.item_desc.setTextColor(color)
        holder.itemView.item_desc.backgroundTintList = ColorStateList.valueOf(color)
    }
}