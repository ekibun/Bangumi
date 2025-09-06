package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.like_list
import kotlinx.android.synthetic.main.item_timeline.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.ui.say.SayActivity
import soko.ekibun.bangumi.ui.topic.LikeAdapter
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HtmlUtil

/**
 * 时间线Adapter
 * @constructor
 */
class TimeLineAdapter(data: MutableList<TimeLine>? = null) :
    BaseSectionQuickAdapter<TimeLine, BaseViewHolder>
        (R.layout.item_episode_header, R.layout.item_timeline, data), LoadMoreModule {
    override fun convertHeader(helper: BaseViewHolder, item: TimeLine) {
        helper.setText(R.id.item_header, item.header)
    }

    override fun convert(holder: BaseViewHolder, item: TimeLine) {
        //say
        holder.itemView.item_layout.setOnClickListener {
            SayActivity.startActivity(holder.itemView.context, item.t?.say ?: return@setOnClickListener)
        }
        if(holder.itemView.like_list.adapter !is LikeAdapter) {
            holder.itemView.like_list.adapter = LikeAdapter()
            holder.itemView.like_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            holder.itemView.like_list.isNestedScrollingEnabled = false
        }
        (holder.itemView.like_list.adapter as LikeAdapter).setList(item.t?.say?.likes)

        holder.itemView.item_layout.isClickable = item.t?.say != null
        //action
        holder.itemView.item_action.text = HtmlUtil.html2span(item.t?.action ?: "")
        holder.itemView.item_action.movementMethod =
            if (holder.itemView.item_layout.isClickable) null else LinkMovementMethod.getInstance()
        //del
        holder.itemView.item_del.visibility = if (item.t?.delUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        holder.itemView.item_del.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context).setMessage(R.string.timeline_dialog_remove)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    (holder.itemView.context as BaseActivity).subscribe(
                        key = item.t?.delUrl
                    ) {
                        TimeLine.removeTimeLine(item)
                        val index = holder.layoutPosition
                        val removeHeader =
                            getItemOrNull(index - 1)?.isHeader == true && getItemOrNull(index + 1)?.isHeader == true
                        removeAt(index)
                        if (removeHeader) removeAt(index - 1)
                    }
                }.show()
        }
        //collectStar
        holder.itemView.item_rate.visibility = if (item.t?.collectStar == 0) View.GONE else View.VISIBLE
        holder.itemView.item_rate.rating = (item.t?.collectStar ?: 0) * 0.5f
        //content
        holder.itemView.item_content.visibility = if (item.t?.content.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.itemView.item_content.text = item.t?.content ?: ""
        holder.itemView.item_content.setOnClickListener {
            WebActivity.launchUrl(holder.itemView.context, item.t?.contentUrl ?: return@setOnClickListener, "")
        }
        holder.itemView.item_content.isClickable = item.t?.contentUrl != null
        //time
        holder.itemView.item_time.text = item.t?.time

        val userImage = Images.getImage(item.t?.user?.avatar)
        holder.itemView.item_avatar.visibility =
            if (userImage.isEmpty() || getItemOrNull(holder.adapterPosition - 1)?.t?.user?.username == item.t?.user?.username) View.INVISIBLE else View.VISIBLE
        holder.itemView.item_avatar.setOnClickListener {
            WebActivity.launchUrl(holder.itemView.context, item.t?.user?.url, "")
        }
        if (userImage.isNotEmpty())
            GlideUtil.with(holder.itemView.item_avatar)
                ?.load(userImage)
                ?.apply(
                    RequestOptions.circleCropTransform().error(R.drawable.err_404)
                        .placeholder(R.drawable.placeholder_round)
                )
                ?.into(holder.itemView.item_avatar)
    }

}