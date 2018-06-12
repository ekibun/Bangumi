package soko.ekibun.bangumi.ui.video

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class TopicAdapter(data: MutableList<Subject.TopicBean>? = null) :
        BaseQuickAdapter<Subject.TopicBean, BaseViewHolder>(R.layout.item_topic, data) {

    override fun convert(helper: BaseViewHolder, item: Subject.TopicBean) {
        helper.setText(R.id.item_user, "${item.user?.nickname}@${item.user?.id}")
        helper.setText(R.id.item_tag, item.title)
        helper.setText(R.id.item_comment, helper.itemView.context.getString(R.string.phrase_reply,item.replies))
        Glide.with(helper.itemView)
                .load(item.user?.avatar?.large)
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)
    }
}