package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Topic

/**
 * 讨论板Adapter
 * @constructor
 */
class TopicAdapter(data: MutableList<Topic>? = null) :
        BaseQuickAdapter<Topic, BaseViewHolder>(R.layout.item_subject_topic, data) {

    override fun convert(holder: BaseViewHolder, item: Topic) {
        @SuppressLint("SetTextI18n")
        holder.itemView.item_user.text = item.user?.nickname
        holder.itemView.item_time.text = item.time
        holder.itemView.item_title.text = item.title
        holder.itemView.item_comment.text = holder.itemView.context.getString(R.string.phrase_reply, item.replyCount)
    }
}