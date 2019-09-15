package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.text.Editable
import android.text.Spanned
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.util.TextUtil

class TopicPresenter(private val context: TopicActivity) {
    private val topicView = TopicView(context)

    init {
        context.item_swipe.setOnRefreshListener {
            getTopic()
        }
    }

    fun getTopic(scrollPost: String = "") {
        context.item_swipe.isRefreshing = true
        Bangumi.getTopic(context.openUrl).enqueue(ApiHelper.buildCallback({ topic ->
            processTopic(topic, scrollPost)
        }) { context.item_swipe.isRefreshing = false })
    }

    private fun processTopic(topic: Topic, scrollPost: String) {
        context.btn_reply.setCompoundDrawablesWithIntrinsicBounds(
                if (!topic.lastview.isNullOrEmpty()) ResourceUtil.getDrawable(context, R.drawable.ic_edit) else null,//left
                null,
                if (!topic.lastview.isNullOrEmpty()) ResourceUtil.getDrawable(context, R.drawable.ic_send) else null,//right
                null)
        context.btn_reply.setOnClickListener {
            if (!topic.lastview.isNullOrEmpty()) showReplyPopupWindow(topic)
            else if (!topic.errorLink.isNullOrEmpty()) WebActivity.launchUrl(context, topic.errorLink, "")
        }
        topicView.processTopic(topic, scrollPost) { v, position ->
            val post = topicView.adapter.data[position]
            when (v.id) {
                R.id.item_avatar ->
                    WebActivity.launchUrl(v.context, "${Bangumi.SERVER}/user/${post.username}")
                R.id.item_reply -> {
                    showReplyPopupWindow(topic, post)
                }
                R.id.item_del -> {
                    AlertDialog.Builder(context).setMessage(R.string.reply_dialog_remove)
                            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                                if (post.floor == 1) {
                                    Bangumi.removeTopic(topic).enqueue(ApiHelper.buildCallback<Boolean>({
                                        if (it) context.finish()
                                    }) {})
                                } else {
                                    Bangumi.removeTopicReply(post).enqueue(ApiHelper.buildCallback<Boolean>({
                                        val data = ArrayList(topicView.adapter.data)
                                        data.removeAll { topicPost -> topicPost.pst_id == post.pst_id }
                                        topicView.setNewData(data)
                                    }) {})
                                }
                            }.show()
                }
                R.id.item_edit -> {
                    buildPopupWindow(context.getString(if (post.floor == 1) R.string.parse_hint_modify_topic else R.string.parse_hint_modify_post, topic.title), html = post.pst_content) { text, send ->
                        if (send) {
                            Bangumi.editTopicReply(topic, post, TextUtil.span2bbcode(text as Spanned)).enqueue(ApiHelper.buildCallback({
                                getTopic(post.pst_id)
                            }))
                        }
                    }
                }
            }
        }
    }

    private fun buildPopupWindow(hint: String = "", draft: Editable? = null, html: String = "", callback: (Editable?, Boolean) -> Unit) {
        val dialog = ReplyDialog()
        dialog.hint = hint
        dialog.draft = draft
        dialog.html = html
        dialog.callback = callback
        dialog.show(context.supportFragmentManager, "reply")
    }

    private val drafts = HashMap<String, Editable>()
    @SuppressLint("InflateParams")
    private fun showReplyPopupWindow(topic: Topic, post: TopicPost? = null) {
        val draftId = post?.pst_id ?: "topic"
        val hint = post?.let { context.getString(R.string.parse_hint_reply_post, post.nickname) }
                ?: context.getString(R.string.parse_hint_reply_topic, topic.title)
        buildPopupWindow(hint, drafts[draftId]) { inputString, send ->
            if (send) {
                Bangumi.replyTopic(topic, post, TextUtil.span2bbcode(inputString as Spanned)).enqueue(ApiHelper.buildCallback<List<TopicPost>>({
                    topicView.setNewData(it)
                }) {})
            } else {
                inputString?.let { drafts[draftId] = it }
            }
        }
    }

}