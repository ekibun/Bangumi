package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_blog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil

class BlogAdapter(data: MutableList<Subject.Blog>? = null) :
        BaseQuickAdapter<Subject.Blog, BaseViewHolder>(R.layout.item_blog, data) {

    override fun convert(helper: BaseViewHolder, item: Subject.Blog) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = item.user?.nickname
        helper.itemView.item_time.text = item.time
        helper.itemView.item_title.text = item.title
        helper.itemView.item_summary.text= item.summary
        helper.itemView.item_comment.text = helper.itemView.context.getString(R.string.phrase_reply,item.replies)
        GlideUtil.with(helper.itemView.item_avatar)
                ?.load(item.image)
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.apply(RequestOptions.circleCropTransform())
                ?.into(helper.itemView.item_avatar)
    }
}