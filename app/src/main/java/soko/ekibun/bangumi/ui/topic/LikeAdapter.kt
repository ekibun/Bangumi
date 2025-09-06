package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_tag.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 贴贴Adapter
 * @constructor
 */
class LikeAdapter(data: MutableList<TopicPost.Like>? = null) :
        BaseQuickAdapter<TopicPost.Like, BaseViewHolder>(R.layout.item_tag, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: TopicPost.Like) {
        holder.itemView.item_tag_del.visibility = View.GONE
        holder.itemView.item_emoji.visibility = View.VISIBLE
        GlideUtil.with(holder.itemView.item_emoji)
            ?.load(Images.small(Bangumi.parseUrl(item.image)))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(holder.itemView.item_emoji)
        holder.itemView.item_tag_name.visibility = View.GONE
        holder.itemView.item_tag_name.text = item.value.toString()
        // holder.itemView.isSelected = hasTag(item.first)
        holder.itemView.item_tag_count.text = item.total.toString()
    }
}