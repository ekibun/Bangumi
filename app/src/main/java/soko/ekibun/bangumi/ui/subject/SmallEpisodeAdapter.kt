package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 剧集列表Adapter
 * @constructor
 */
class SmallEpisodeAdapter(data: MutableList<Episode>? = null) :
        BaseQuickAdapter<Episode, BaseViewHolder>
        (R.layout.item_episode_small, data) {

    override fun convert(holder: BaseViewHolder, item: Episode) {
        holder.setText(R.id.item_title, item.parseSort())
        holder.setText(R.id.item_desc, if (item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        val color = ResourceUtil.resolveColorAttr(
            holder.itemView.context,
            when (item.progress) {
                Episode.PROGRESS_WATCH -> R.attr.colorPrimary
                else -> android.R.attr.textColorSecondary
            }
        )
        holder.itemView.item_title.setTextColor(color)
        holder.itemView.item_desc.setTextColor(color)
        holder.itemView.item_badge.visibility = if (item.progress in arrayOf(
                Episode.PROGRESS_WATCH,
                Episode.PROGRESS_DROP,
                Episode.PROGRESS_QUEUE
            )
        ) View.VISIBLE else View.INVISIBLE
        holder.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(
            ResourceUtil.resolveColorAttr(
                holder.itemView.context,
                when (item.progress) {
                    in listOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_QUEUE) -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                }
            )
        )
        holder.itemView.item_badge.text = mapOf(
            Episode.PROGRESS_WATCH to R.string.episode_status_watch,
            Episode.PROGRESS_DROP to R.string.episode_status_drop,
            Episode.PROGRESS_QUEUE to R.string.episode_status_wish
        )[item.progress ?: ""]?.let { holder.itemView.context.getString(it) } ?: ""
        holder.itemView.backgroundTintList = ColorStateList.valueOf(color)
    }
}