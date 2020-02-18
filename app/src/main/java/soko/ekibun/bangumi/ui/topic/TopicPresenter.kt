package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.text.Html
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HtmlTagHandler
import soko.ekibun.bangumi.util.TextUtil
import kotlin.collections.set

/**
 * 帖子Presenter
 * @property context TopicActivity
 * @property topicView TopicView
 * @property dataCacheModel DataCacheModel
 * @property topic Topic
 * @property loadMoreFail Boolean?
 * @property drafts HashMap<String, String>
 * @constructor
 */
class TopicPresenter(private val context: TopicActivity, topic: Topic, scrollPost: String) {
    val topicView = TopicView(context)

    val dataCacheModel by lazy { App.get(context).dataCacheModel }

    var topic: Topic

    init {
        // 读取缓存
        DataCacheModel.merge(topic, dataCacheModel.get(topic.cacheKey))
        this.topic = topic
        processTopic(topic, scrollPost, isCache = true)

        getTopic(scrollPost)

        context.item_swipe.setOnRefreshListener {
            getTopic()
        }
        topicView.adapter.setLoadMoreView(BrvahLoadMoreView())
        topicView.adapter.setOnLoadMoreListener({ if (loadMoreFail == true) getTopic() }, context.item_list)
    }

    private var loadMoreFail: Boolean? = null
    /**
     * 读取帖子
     * @param scrollPost String
     */
    fun getTopic(scrollPost: String = "") {
        loadMoreFail = null
        context.item_swipe.isRefreshing = true

        Topic.getTopicSax(topic, { data ->
            context.runOnUiThread {
                topicView.processTopic(data, scrollPost, true)
            }
        }, { post ->
            context.runOnUiThread {
                val related = topicView.adapter.data.find { it.pst_id == post.relate }
                if (post.sub_floor > related?.subItems?.size ?: 0) {
                    related?.subItems?.add(post)
                } else if (post.sub_floor > 0) {
                    related?.subItems?.set(post.sub_floor - 1, post)
                }
                val oldPostIndex = topicView.adapter.data.indexOfFirst { it.pst_id == post.pst_id }
                if (oldPostIndex >= 0) {
                    val oldPost = topicView.adapter.data.getOrNull(oldPostIndex)
                    post.isExpanded = oldPost?.isExpanded ?: true
                    post.subItems = oldPost?.subItems ?: post.subItems ?: ArrayList()
                    topicView.adapter.setData(oldPostIndex, post)
                } else {
                    topicView.adapter.addData(post)
                }
            }
        }).enqueue(ApiHelper.buildCallback({ topic ->
            processTopic(topic, scrollPost)
        }) {
            loadMoreFail = it != null
            context.item_swipe.isRefreshing = false
            topicView.adapter.loadMoreFail()
        })
    }

    private fun processTopic(topic: Topic, scrollPost: String, isCache: Boolean = false) {
        context.btn_reply.setOnClickListener {
            if (!topic.lastview.isNullOrEmpty()) showReplyPopupWindow(topic)
            else if (topic.error != null) WebActivity.launchUrl(context, topic.error?.second, "")
        }
        topicView.processTopic(topic, scrollPost, isCache = isCache) { v, position ->
            val post = topicView.adapter.data[position]
            when (v.id) {
                R.id.item_avatar ->
                    WebActivity.startActivity(v.context, "${Bangumi.SERVER}/user/${post.username}")
                R.id.item_reply -> {
                    showReplyPopupWindow(topic, post)
                }
                R.id.item_del -> {
                    AlertDialog.Builder(context).setMessage(R.string.reply_dialog_remove)
                            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                                if (post.floor == if (topic.blog == null) 1 else 0) {
                                    Topic.remove(topic).enqueue(ApiHelper.buildCallback<Boolean>({
                                        if (it) context.finish()
                                    }) {})
                                } else {
                                    TopicPost.remove(post).enqueue(ApiHelper.buildCallback<Boolean>({
                                        val data = ArrayList(topic.replies)
                                        data.removeAll { topicPost -> topicPost.pst_id == post.pst_id }
                                        topicView.setNewData(data, topic)
                                        topicView.adapter.loadMoreEnd()
                                    }) {})
                                }
                            }.show()
                }
                R.id.item_edit -> {
                    buildPopupWindow(
                        context.getString(
                            if (post.floor == if (topic.blog == null) 1 else 0) R.string.parse_hint_modify_topic else R.string.parse_hint_modify_post,
                            topic.title
                        ),
                        title = if (post.floor == if (topic.blog == null) 1 else 0) topic.title else null,
                        html = post.pst_content
                    ) { text, title, send ->
                        if (send) {
                            if (post.floor == if (topic.blog == null) 1 else 0) {
                                Topic.edit(topic, title ?: "", text ?: "")
                            } else {
                                TopicPost.edit(post, text ?: "")
                            }.enqueue(ApiHelper.buildCallback({
                                getTopic(post.pst_id)
                            }))
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildPopupWindow(
        hint: String = "",
        draft: String? = null,
        title: String? = null,
        html: String = "",
        callback: (String?, String?, Boolean) -> Unit
    ) {
        ReplyDialog.showDialog(
            context.supportFragmentManager,
            hint = hint,
            draft = draft ?: {
                TextUtil.span2bbcode(Html.fromHtml(ReplyDialog.parseHtml(html), null, HtmlTagHandler()))
            }(),
            title = title,
            callback = callback
        )
    }

    private val drafts = HashMap<String, String>()
    @SuppressLint("InflateParams")
    private fun showReplyPopupWindow(topic: Topic, post: TopicPost? = null) {
        val draftId = post?.pst_id ?: "topic"
        val hint = post?.let { context.getString(R.string.parse_hint_reply_post, post.nickname) }
                ?: context.getString(R.string.parse_hint_reply_topic, topic.title)
        buildPopupWindow(hint, drafts[draftId]) { inputString, _, send ->
            if (send) {
                Topic.reply(topic, post, inputString ?: "").enqueue(ApiHelper.buildCallback<List<TopicPost>>({
                    topicView.setNewData(it, topic)
                    topicView.adapter.loadMoreEnd()
                }) {})
            } else {
                inputString?.let { drafts[draftId] = it }
            }
        }
    }
}