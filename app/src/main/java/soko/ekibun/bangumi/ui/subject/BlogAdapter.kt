package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_blog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 日志Adapter
 * @constructor
 */
class BlogAdapter(data: MutableList<Topic>? = null) :
        BaseQuickAdapter<Topic, BaseViewHolder>(R.layout.item_blog, data) {

    override fun convert(helper: BaseViewHolder, item: Topic) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = item.user?.nickname
        helper.itemView.item_time.text = item.time
        helper.itemView.item_title.text = item.title
        helper.itemView.item_summary.text = item.blog?.pst_content
        helper.itemView.item_comment.text = helper.itemView.context.getString(R.string.phrase_reply, item.replyCount)
        GlideUtil.with(helper.itemView.item_avatar)
            ?.load(Images.getImage(item.image, helper.itemView.context))
            ?.apply(RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round))
                ?.into(helper.itemView.item_avatar)
    }
}