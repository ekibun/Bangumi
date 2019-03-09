package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_comment.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Comment

class CommentAdapter(data: MutableList<Comment>? = null) :
        BaseQuickAdapter<Comment, BaseViewHolder>(R.layout.item_comment, data) {

    override fun convert(helper: BaseViewHolder, item: Comment) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = "${item.user?.nickname}@${item.user?.username}"
        helper.itemView.item_time.text = item.time
        helper.itemView.item_title.text = item.comment
        //helper.itemView.item_rate.visibility = if(item.rate == 0) View.GONE else View.VISIBLE
        helper.itemView.item_rate.rating = item.rate * 0.5f
        Glide.with(helper.itemView.item_avatar)
                .load(item.user?.avatar?.small)
                .apply(RequestOptions.errorOf(R.drawable.err_404))
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)
    }
}