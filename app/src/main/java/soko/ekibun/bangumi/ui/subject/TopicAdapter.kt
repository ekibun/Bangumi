package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

/**
 * 讨论板Adapter
 */
class TopicAdapter(data: MutableList<Subject.Topic>? = null) :
        BaseQuickAdapter<Subject.Topic, BaseViewHolder>(R.layout.item_subject_topic, data) {

    override fun convert(helper: BaseViewHolder, item: Subject.Topic) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = item.user?.nickname
        helper.itemView.item_time.text = item.time
        helper.itemView.item_title.text = item.title
        helper.itemView.item_comment.text = helper.itemView.context.getString(R.string.phrase_reply,item.replies)
    }
}