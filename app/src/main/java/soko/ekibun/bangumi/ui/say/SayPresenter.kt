package soko.ekibun.bangumi.ui.say

import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil

class SayPresenter(private val context: SayActivity, say: Say) {
    val sayView = SayView(context)

    var say: Say
    val dataCacheModel by lazy { App.app.dataCacheModel }

    init {
        DataCacheModel.merge(say, dataCacheModel.get(say.cacheKey))
        this.say = say
        sayView.processSay(say, isCache = true)
        getSay()

        context.item_swipe.setOnRefreshListener {
            getSay()
        }
        sayView.adapter.loadMoreModule.setOnLoadMoreListener { if (loadMoreFail == true) getSay() }

        var draft: String? = null
        context.btn_reply.setOnClickListener {
            if (HttpUtil.formhash.isNotEmpty()) showReply(say, say.user, draft) { draft = it }
            else WebActivity.launchUrl(context, say.url, "")
        }
        sayView.adapter.setOnItemChildClickListener { _, _, position ->
            WebActivity.launchUrl(context, sayView.adapter.data[position].t.user.url, "")
        }
        sayView.adapter.setOnItemChildLongClickListener { _, _, position ->
            sayView.adapter.data[position].t.user.let {
                showReply(say, it, "@${it.username} ") { draft = it }
            }

            true
        }
    }

    fun updateHistory() {
        context.subscribe { HistoryModel.addHistory(say) }
    }

    private var loadMoreFail: Boolean? = null

    fun getSay() {
        sayView.adapter.loadMoreModule.loadMoreComplete()
        loadMoreFail = null
        context.item_swipe.isRefreshing = true
        context.subscribe({
            loadMoreFail = true
            sayView.adapter.loadMoreModule.loadMoreFail()
        }, {
            context.item_swipe.isRefreshing = false
        }, SAY_CALL) {
            val updatePost = { post: Say.SayReply ->
                val index = sayView.adapter.data.indexOfFirst { it.t.index == post.index }
                if (index < 0) {
                    val insertIndex = sayView.adapter.data.indexOfLast { it.t.index < post.index }
                    sayView.adapter.addData(insertIndex + 1, SayAdapter.SaySection(false, post))
                } else sayView.adapter.setData(index, SayAdapter.SaySection(false, post))
            }
            Say.getSaySax(say, {
                updateHistory()
                sayView.processSay(say, true)
                updatePost(it)
            }) {
                it.forEach(updatePost)
            }
            sayView.processSay(say)
            dataCacheModel.set(say.cacheKey, say)
        }
    }

    private fun showReply(say: Say, user: UserInfo?, draft: String?, updateDraft: (String?) -> Unit) {
        val self = UserModel.current() ?: return
        ReplyDialog.showDialog(
            context.supportFragmentManager,
            hint = context.getString(R.string.parse_hint_reply_topic, user?.nickname),
            draft = draft
        ) { content, _, send ->
            updateDraft(content)
            if (content != null && send) {
                context.subscribe(key = SAY_REPLY_CALL) {
                    Say.reply(say, content)
                    updateDraft(null)
                    say.replies = (say.replies ?: ArrayList()).let { replies ->
                        replies.plus(
                            Say.SayReply(
                                user = self,
                                message = content,
                                index = replies.size
                            )
                        )
                    }
                    sayView.processSay(say)
                    (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        sayView.adapter.itemCount,
                        0
                    )
                    getSay()
                }
            }
        }
    }

    companion object {
        const val SAY_CALL = "bangumi_say"
        const val SAY_REPLY_CALL = "bangumi_say_reply"
    }
}