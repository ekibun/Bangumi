package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Topic

/**
 * 讨论板Adapter
 * @constructor
 */
class TopicAdapter(data: MutableList<Topic>? = null) :
        BaseQuickAdapter<Topic, BaseViewHolder>(R.layout.item_subject_topic, data) {

    override fun convert(helper: BaseViewHolder, item: Topic) {
        @SuppressLint("SetTextI18n")
        helper.itemView.item_user.text = item.user?.nickname
        helper.itemView.item_time.text = item.time
        helper.itemView.item_title.text = item.title
        helper.itemView.item_comment.text = helper.itemView.context.getString(R.string.phrase_reply, item.replyCount)
    }
}