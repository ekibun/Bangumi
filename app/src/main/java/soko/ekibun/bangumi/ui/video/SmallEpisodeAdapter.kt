package soko.ekibun.bangumi.ui.video

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.util.ResourceUtil

class SmallEpisodeAdapter(data: MutableList<Episode>? = null) :
        BaseQuickAdapter<Episode, BaseViewHolder>
        (R.layout.item_episode_small, data) {

    override fun convert(helper: BaseViewHolder, item: Episode) {
        helper.setText(R.id.item_title, item.parseSort())
        helper.setText(R.id.item_desc, if(item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.progress != null -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        val alpha = if((item.status?:"") in listOf("Air"))1f else 0.6f
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_layout.backgroundTintList = ColorStateList.valueOf(color)
        helper.itemView.item_layout.alpha = alpha
    }
}