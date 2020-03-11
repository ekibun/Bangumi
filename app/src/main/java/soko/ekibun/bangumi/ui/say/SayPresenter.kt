package soko.ekibun.bangumi.ui.say

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.web.WebActivity

class SayPresenter(private val context: SayActivity, say: Say) {
    val sayView = SayView(context)

    var say: Say
    val dataCacheModel by lazy { App.get(context).dataCacheModel }

    init {
        val self = say.self
        DataCacheModel.merge(say, dataCacheModel.get(say.cacheKey))
        say.self = self
        this.say = say
        sayView.processSay(say, isCache = true)
        getSay()

        context.item_swipe.setOnRefreshListener {
            getSay()
        }
        sayView.adapter.setLoadMoreView(BrvahLoadMoreView())
        sayView.adapter.setOnLoadMoreListener({ if (loadMoreFail == true) getSay() }, context.item_list)
    }

    private var loadMoreFail: Boolean? = null

    fun getSay() {
        sayView.adapter.loadMoreComplete()
        loadMoreFail = null
        context.item_swipe.isRefreshing = true
        Say.getSaySax(say, { data ->
            context.runOnUiThread {
                sayView.processSay(data, true)
            }
        }, { index, post ->
            context.runOnUiThread {
                val say = SayAdapter.SaySection(
                    isHeader = sayView.adapter.data.getOrNull(index - 1)?.t?.user?.username != post.user.username,
                    reply = post
                )
                if (index < sayView.adapter.data.size) sayView.adapter.setData(index, say)
                else sayView.adapter.addData(say)
            }

        }).enqueue(ApiHelper.buildCallback({ say ->
            sayView.processSay(say)
            dataCacheModel.set(say.cacheKey, say)

            var draft: String? = null
            context.btn_reply.setOnClickListener {
                val self = say.self
                if (self != null) showReply(say, draft) { draft = it }
                else WebActivity.launchUrl(context, say.url, "")
            }
            sayView.adapter.setOnItemChildClickListener { _, _, position ->
                WebActivity.launchUrl(context, sayView.adapter.data[position].t.user.url, "")
            }
            sayView.adapter.setOnItemChildLongClickListener { _, _, position ->
                showReply(say, "@${sayView.adapter.data[position].t.user.username} ") { draft = it }
                true
            }

        }) {
            loadMoreFail = it != null
            context.item_swipe.isRefreshing = false
            sayView.adapter.loadMoreFail()
        })
    }

    private fun showReply(say: Say, draft: String?, updateDraft: (String?) -> Unit) {
        val self = say.self ?: return
        ReplyDialog.showDialog(
            context.supportFragmentManager,
            hint = context.getString(R.string.parse_hint_reply_topic, say.user.nickname) ?: "",
            draft = draft
        ) { content, _, send ->
            if (content != null && send) {
                Say.reply(say, content).enqueue(ApiHelper.buildCallback({
                    if (it) {
                        updateDraft(null)
                        say.replies = (say.replies ?: ArrayList()).plus(
                            Say.SayReply(
                                user = self,
                                message = content
                            )
                        )
                        sayView.processSay(say)
                        (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            sayView.adapter.itemCount,
                            0
                        )
                        getSay()
                    } else Toast.makeText(context, R.string.hint_submit_error, Toast.LENGTH_LONG).show()
                }) { })
            } else updateDraft(content)
        }
    }
}