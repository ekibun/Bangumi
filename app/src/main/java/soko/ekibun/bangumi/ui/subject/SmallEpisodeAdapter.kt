package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.util.ResourceUtil

class SmallEpisodeAdapter(data: MutableList<Episode>? = null) :
        BaseQuickAdapter<Episode, BaseViewHolder>
        (R.layout.item_episode_small, data) {

    override fun convert(helper: BaseViewHolder, item: Episode) {
        helper.setText(R.id.item_title, item.parseSort(helper.itemView.context))
        helper.setText(R.id.item_desc, if(item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.progress?.status?.url_name?:"" == SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_badge.visibility = if(item.progress != null) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.progress?.status?.url_name?:"" in listOf(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE) -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                }))
        helper.itemView.item_badge.text = item.progress?.status?.cn_name?:""
        helper.itemView.item_ep_box.backgroundTintList = ColorStateList.valueOf(color)
    }
}