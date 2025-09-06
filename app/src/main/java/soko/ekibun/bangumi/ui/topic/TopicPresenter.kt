package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HtmlUtil
import kotlin.collections.set

/**
 * 帖子Presenter
 * @property context TopicActivity
 * @property topicView TopicView
 * @property dataCacheModel DataCacheModel
 * @property topic Topic
 * @property drafts HashMap<String, String>
 * @constructor
 */
class TopicPresenter(private val context: TopicActivity, topic: Topic, scrollPost: String) {
    val topicView = TopicView(context)

    val dataCacheModel by lazy { App.app.dataCacheModel }

    var topic: Topic

    val likePopup by lazy {
        val likePopupView = RecyclerView(context)
        likePopupView.layoutManager = GridLayoutManager(context, 4)
        val likeEmojis = TopicPost.Like.emojiWrap.map { it.key.toString() to Bangumi.parseUrl(it.value) }.toMutableList()
        val adapter = EmojiAdapter(likeEmojis)
        likePopupView.adapter = adapter
        val popupWindow = PopupWindow(likePopupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,true)
        popupWindow.setBackgroundDrawable(
            AppCompatResources.getDrawable(context, R.drawable.abc_popup_background_mtrl_mult)
        )
        popupWindow.isOutsideTouchable = true
        { view: View, cb: (String)->Unit ->
            popupWindow.showAsDropDown(view)
            adapter.setOnItemChildClickListener { _, _, position ->
                popupWindow.dismiss()
                cb(likeEmojis[position].first)
            }
        }
    }

    init {
        // 读取缓存
        DataCacheModel.merge(topic, dataCacheModel.get(topic.cacheKey))
        this.topic = topic
        processTopic(topic, scrollPost, isCache = true)

        getTopic(scrollPost)

        context.item_swipe.setOnRefreshListener {
            getTopic()
        }
        topicView.adapter.loadMoreModule.isAutoLoadMore = false
        topicView.adapter.loadMoreModule.setOnLoadMoreListener { getTopic() }
    }

    fun updateHistory() {
        context.subscribe { HistoryModel.addHistory(topic) }
    }

    /**
     * 读取帖子
     * @param scrollPost String
     */
    fun getTopic(scrollPost: String = "") {
//        topicView.adapter.loadMoreModule.loadMoreToLoading()
        context.item_swipe.isRefreshing = true

        context.subscribe({
            topicView.adapter.loadMoreModule.loadMoreFail()
        }, {
            context.item_swipe.isRefreshing = false
        }, "bangumi_topic") {
            Topic.getTopicSax(topic, {
                topicView.processTopic(topic, scrollPost, true)
                updateHistory()
            }, { data ->
                data.forEach { post ->
                    val index = topicView.adapter.data.indexOfFirst { (it as TopicPost).pst_id == post.pst_id }
                    if (index < 0) {
                        val insertIndex =
                            topicView.adapter.data.indexOfLast { (it as TopicPost).floor < post.floor }
                        topicView.adapter.addData(insertIndex + 1, post)
                    } else topicView.adapter.setData(index, post)
                }
            })
            processTopic(topic, scrollPost)
            dataCacheModel.set(topic.cacheKey, topic)
            topicView.adapter.loadMoreModule.loadMoreEnd()
        }
    }

    private fun processTopic(topic: Topic, scrollPost: String, isCache: Boolean = false) {
        context.btn_reply.setOnClickListener {
            if (!topic.lastview.isNullOrEmpty()) showReplyPopupWindow(topic)
            else if (topic.error != null) WebActivity.launchUrl(context, topic.error?.second, "")
        }
        topicView.processTopic(topic, scrollPost, isCache = isCache) { v, position ->
            val post = topicView.adapter.data[position] as TopicPost
            when (v.id) {
                R.id.item_avatar ->
                    WebActivity.startActivity(v.context, "${Bangumi.SERVER}/user/${post.username}")
                R.id.item_reply -> {
                    showReplyPopupWindow(topic, post)
                }
                R.id.item_dolike -> {
                    likePopup(v) { value ->
                        context.subscribe {
                            TopicPost.Like.dolike(post.likeType, topic.id, post.pst_id, value)
                            val user = UserModel.current()?: return@subscribe
                            val like = post.likes?.firstOrNull{ it.value.toString() == value }
                            if(like == null) {
                                // 删除其他的，只能留下一个
                                post.likes?.forEach {
                                    val newUsersOther = it.users.toMutableList()
                                    newUsersOther.removeAll { it.username == user.username }
                                    it.total = newUsersOther.size
                                    it.users = newUsersOther
                                }
                                post.likes = post.likes.orEmpty() + TopicPost.Like(
                                    value = value.toIntOrNull()?: 0,
                                    type = post.likeType,
                                    main_id = topic.id,
                                    total = 1,
                                    users = listOf(user)
                                )
                            } else {
                                val newUsers = like.users.toMutableList()
                                if(!newUsers.removeAll { it.username == user.username }){
                                    // 删除其他的，只能留下一个
                                    post.likes?.forEach {
                                        val newUsersOther = it.users.toMutableList()
                                        newUsersOther.removeAll { it.username == user.username }
                                        it.total = newUsersOther.size
                                        it.users = newUsersOther
                                    }
                                    UserModel.current()?.let { newUsers.add(it) }
                                }
                                like.total = newUsers.size
                                like.users = newUsers
                            }
                            topicView.adapter.notifyItemChanged(position)
                        }
                    }
                }
                R.id.item_del -> {
                    AlertDialog.Builder(context).setMessage(R.string.reply_dialog_remove)
                            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                                if (post.floor == if (topic.blog == null) 1 else 0) {
                                    context.subscribe {
                                        Topic.remove(topic)
                                        context.finish()
                                    }
                                } else {
                                    context.subscribe {
                                        TopicPost.remove(post)
                                        if (!post.isSub) {
                                            topicView.adapter.removeAt(position)
                                        } else {
                                            val parentPos = topicView.adapter.findParentNode(position)
                                            topicView.adapter.nodeRemoveData(
                                                topicView.adapter.getItem(parentPos),
                                                post
                                            )
                                            topicView.adapter.notifyItemChanged(parentPos)
                                        }
                                    }
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
                            context.subscribe {
                                if (post.floor == if (topic.blog == null) 1 else 0) {
                                    Topic.edit(topic, title ?: "", text ?: "")
                                } else {
                                    TopicPost.edit(post, text ?: "")
                                }
                                getTopic(post.pst_id)
                            }
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
            draft = draft ?: { HtmlUtil.span2bbcode(HtmlUtil.html2span(html)) }(),
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
            inputString?.let { drafts[draftId] = it }
            if (send) {
                context.subscribe {
                    val reply = Topic.reply(topic, post, inputString ?: "")
                    if (inputString == drafts[draftId]) drafts[draftId] = ""
                    var lastFloor = (topicView.adapter.data.lastOrNull() as? TopicPost)?.floor ?: 0
                    reply.main?.values?.forEach { newPost ->
                        val oldPostIndex =
                            topicView.adapter.data.indexOfFirst { (it as TopicPost).pst_id == newPost.pst_id }
                        newPost.relate = newPost.pst_id
                        if (oldPostIndex < 0) {
                            lastFloor += 1
                            newPost.floor = lastFloor
                            topicView.adapter.addData(newPost)
                        } else {
                            val oldPost = topicView.adapter.data[oldPostIndex] as TopicPost
                            newPost.floor = oldPost.floor
                            newPost.children += oldPost.children
                            topicView.adapter.setData(oldPostIndex, newPost)
                        }
                    }
                    reply.sub?.forEach { (key, posts) ->
                        val parentIndex = topicView.adapter.data.indexOfFirst { (it as TopicPost).pst_id == key }
                        val parent = (topicView.adapter.data.getOrNull(parentIndex) as? TopicPost) ?: return@forEach
                        var lastSubFloor = parent.children.size
                        posts.forEach { subPost ->
                            val oldPostIndex = parent.children.indexOfFirst { it.pst_id == subPost.pst_id }
                            subPost.relate = key
                            if (oldPostIndex < 0) {
                                lastSubFloor += 1
                                subPost.floor = parent.floor
                                subPost.sub_floor = lastSubFloor
                                topicView.adapter.nodeAddData(parent, subPost)
                            } else {
                                subPost.sub_floor = oldPostIndex + 1
                                topicView.adapter.nodeSetData(parent, oldPostIndex, subPost)
                            }
                        }
                        topicView.adapter.notifyItemChanged(parentIndex)
                    }
                    UserModel.current()?.username?.let { username ->
                        if (post != null) reply.sub?.get(post.pst_id)?.lastOrNull { it.username == username }
                        else reply.main?.values?.lastOrNull { it.username == username }
                    }?.let { topicView.scrollToPost(it.pst_id, true) }
                }
            }
        }
    }
}