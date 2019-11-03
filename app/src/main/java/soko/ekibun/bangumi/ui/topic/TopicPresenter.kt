package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.text.Html
import android.text.Spanned
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_topic.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.TextUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

class TopicPresenter(private val context: TopicActivity) {
    private val topicView = TopicView(context)

    init {
        context.item_swipe.setOnRefreshListener {
            getTopic()
        }
        topicView.adapter.setLoadMoreView(BrvahLoadMoreView())
        topicView.adapter.setOnLoadMoreListener({ if (loadMoreFail == true) getTopic() }, context.item_list)
    }

    var loadMoreFail: Boolean? = null
    fun getTopic(scrollPost: String = "") {
        loadMoreFail = null
        context.item_swipe.isRefreshing = true

        if (topicView.adapter.data.isEmpty()) {
            Bangumi.getTopicSax(context.openUrl, { data ->
                val doc = Jsoup.parse(data)
                context.runOnUiThread {
                    topicView.processTopicBefore(
                            title = doc.selectFirst("#pageHeader h1")?.ownText() ?: "",
                            links = LinkedHashMap<String, String>().let { links ->
                                doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                                    links[it.text()] = Bangumi.parseUrl(it.attr("href") ?: "")
                                }
                                links
                            },
                            images = Images(Bangumi.parseImageUrl(doc.selectFirst("#pageHeader img")))
                    )
                }
            }, {
                context.runOnUiThread {
                    val last = topicView.adapter.data.lastOrNull()
                    val floor = (last?.floor ?: 1) + if (it.isSub) 0 else 1
                    val subFloor = 1 + if (it.isSub) last?.sub_floor ?: 0 else 0
                    it.floor = floor
                    it.sub_floor = subFloor
                    it.editable = it.is_self
                    if (it.isSub) topicView.adapter.data.lastOrNull { p -> !p.isSub }?.let { ref ->
                        ref.editable = false
                        ref.addSubItem(it)
                        //topicView.adapter.notifyItemChanged(topicView.adapter.data.indexOf(ref))
                        topicView.adapter.addData(it)
                    } else {
                        it.isExpanded = true
                        topicView.adapter.addData(it)
                    }
                }
            })
        } else {
            Bangumi.getTopic(context.openUrl)
        }.enqueue(ApiHelper.buildCallback({ topic ->
            processTopic(topic, scrollPost)
        }) {
            loadMoreFail = it != null
            context.item_swipe.isRefreshing = false
            topicView.adapter.loadMoreFail()
        })
    }

    private fun processTopic(topic: Topic, scrollPost: String) {
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
                                        topicView.adapter.loadMoreEnd()
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

    private fun buildPopupWindow(hint: String = "", draft: String? = null, html: String = "", callback: (String?, Boolean) -> Unit) {
        val dialog = ReplyDialog()
        dialog.hint = hint
        dialog.draft = draft ?: {
            TextUtil.span2bbcode(Html.fromHtml(ReplyDialog.parseHtml(html)))
        }()
        dialog.bbCode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_bbcode", false)
        dialog.callback = callback
        dialog.show(context.supportFragmentManager, "reply")
    }

    private val drafts = HashMap<String, String>()
    @SuppressLint("InflateParams")
    private fun showReplyPopupWindow(topic: Topic, post: TopicPost? = null) {
        val draftId = post?.pst_id ?: "topic"
        val hint = post?.let { context.getString(R.string.parse_hint_reply_post, post.nickname) }
                ?: context.getString(R.string.parse_hint_reply_topic, topic.title)
        buildPopupWindow(hint, drafts[draftId]) { inputString, send ->
            if (send) {
                Bangumi.replyTopic(topic, post, TextUtil.span2bbcode(inputString as Spanned)).enqueue(ApiHelper.buildCallback<List<TopicPost>>({
                    topicView.setNewData(it)
                    topicView.adapter.loadMoreEnd()
                }) {})
            } else {
                inputString?.let { drafts[draftId] = it }
            }
        }
    }

}