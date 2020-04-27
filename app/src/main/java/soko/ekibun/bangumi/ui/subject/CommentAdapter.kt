package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_comment.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Comment
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 吐槽Adapter
 * @constructor
 */
class CommentAdapter(data: MutableList<Comment>? = null) :
    BaseQuickAdapter<Comment, BaseViewHolder>(R.layout.item_comment, data), LoadMoreModule {

    override fun convert(holder: BaseViewHolder, item: Comment) {
        @SuppressLint("SetTextI18n")
        holder.itemView.item_user.text = "${item.user?.nickname}"
        holder.itemView.item_time.text = item.time
        holder.itemView.item_title.text = item.comment
        //helper.itemView.item_rate.visibility = if(item.rate == 0) View.GONE else View.VISIBLE
        holder.itemView.item_rate.rating = item.rate * 0.5f
        GlideUtil.with(holder.itemView.item_avatar)
            ?.load(Images.small(item.user?.avatar))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.into(holder.itemView.item_avatar)
    }
}