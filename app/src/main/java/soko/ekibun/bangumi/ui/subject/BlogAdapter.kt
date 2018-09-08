package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_blog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.ResourceUtil

class BlogAdapter(data: MutableList<Subject.BlogBean>? = null) :
        BaseQuickAdapter<Subject.BlogBean, BaseViewHolder>(R.layout.item_blog, data) {

    override fun convert(helper: BaseViewHolder, item: Subject.BlogBean) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = "${item.user?.nickname}@${item.user?.id}"
        helper.itemView.item_time.text = ResourceUtil.getTimeInterval(item.timestamp)
        helper.itemView.item_title.text = item.title
        helper.itemView.item_summary.text= item.summary
        helper.itemView.item_comment.text = helper.itemView.context.getString(R.string.phrase_reply,item.replies)
        Glide.with(helper.itemView)
                .load(item.user?.avatar?.small)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)
    }
}