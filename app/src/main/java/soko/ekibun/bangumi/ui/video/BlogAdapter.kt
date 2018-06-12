package soko.ekibun.bangumi.ui.video

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_blog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class BlogAdapter(data: MutableList<Subject.BlogBean>? = null) :
        BaseQuickAdapter<Subject.BlogBean, BaseViewHolder>(R.layout.item_blog, data) {

    override fun convert(helper: BaseViewHolder, item: Subject.BlogBean) {
        helper.setText(R.id.item_title, item.title)
        helper.setText(R.id.item_summary, item.summary)
        helper.setText(R.id.item_comment, "by ${item.user?.nickname}  ${item.dateline}  " + helper.itemView.context.getString(R.string.phrase_reply,item.replies))
        Glide.with(helper.itemView)
                .load(item.user?.avatar?.large)
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)
    }
}