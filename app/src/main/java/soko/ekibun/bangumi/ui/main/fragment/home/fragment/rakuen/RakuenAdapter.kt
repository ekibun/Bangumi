package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 超展开列表Adapter
 * @constructor
 */
class RakuenAdapter(data: MutableList<Topic>? = null) :
        BaseQuickAdapter<Topic, BaseViewHolder>(R.layout.item_topic, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Topic) {
        holder.itemView.item_title.text = item.title
        holder.itemView.item_time.text = "${item.time} (+${item.replyCount})"
        holder.itemView.item_group.visibility = View.GONE
        item.links?.entries?.firstOrNull()?.let {
            holder.itemView.item_group.visibility = View.VISIBLE
            holder.itemView.item_group.text = it.key
        }
        GlideUtil.with(holder.itemView.item_avatar)
            ?.load(Images.small(item.image))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.into(holder.itemView.item_avatar)
    }
}