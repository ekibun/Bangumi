package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.support.v7.app.AlertDialog
import android.webkit.CookieManager
import android.webkit.WebView
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_topic.*
import okhttp3.FormBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.ResourceUtil

class TopicPresenter(private val context: TopicActivity) {
    private val topicView = TopicView(context)

    init{
        context.item_swipe.setOnRefreshListener {
            getTopic()
        }
    }

    private val ua by lazy { WebView(context).settings.userAgentString }
    fun getTopic(scrollPost: String = ""){
        context.item_swipe.isRefreshing = true
        getTopicApi{ topic ->
            if(topic.user_id.isNullOrEmpty()){
                context.item_swipe.isRefreshing = true
                val cookieManager = CookieManager.getInstance()
                ApiHelper.buildHttpCall(Bangumi.SERVER, mapOf("User-Agent" to ua)){ response ->
                    val doc = Jsoup.parse(response.body()?.string()?:"")
                    if(doc.selectFirst(".guest") != null) return@buildHttpCall null
                    response.headers("set-cookie").forEach {
                        cookieManager.setCookie(Bangumi.SERVER, it) }
                    doc.selectFirst("input[name=formhash]")?.attr("value")
                }.enqueue(ApiHelper.buildCallback(context, { hash->
                    if(hash.isNullOrEmpty()) processTopic(topic, scrollPost)
                    else getTopicApi{
                        processTopic(it, scrollPost)
                    }
                }))
            }else processTopic(topic, scrollPost)
        }
    }
    private fun getTopicApi(callback: (Topic)->Unit){
        Bangumi.getTopic(context.openUrl).enqueue(ApiHelper.buildCallback(context, {topic->
            callback(topic)
        }){context.item_swipe.isRefreshing = false})
    }

    private fun processTopic(topic: Topic, scrollPost: String){
        context.item_reply.setCompoundDrawablesWithIntrinsicBounds(
                if(!topic.formhash.isNullOrEmpty()) ResourceUtil.getDrawable(context, R.drawable.ic_edit) else null,//left
                null,
                if(!topic.formhash.isNullOrEmpty()) ResourceUtil.getDrawable(context, R.drawable.ic_send) else null,//right
                null)
        context.item_reply.setOnClickListener {
            topic.formhash?.let{ formhash -> showReplyPopupWindow(topic.post, FormBody.Builder().add("lastview", topic.lastview!!).add("formhash", formhash),"", "回复 ${topic.title}") }?:{
                if(!topic.errorLink.isNullOrEmpty()) WebActivity.launchUrl(context, topic.errorLink, "")
            }()
        }
        topicView.processTopic(topic, scrollPost){v, position ->
            val post = topicView.adapter.data[position]
            when (v.id) {
                R.id.item_avatar ->
                    WebActivity.launchUrl(v.context, "${Bangumi.SERVER}/user/${post.username}")
                R.id.item_reply -> {
                    val body = Jsoup.parse(post.pst_content).body()
                    body.select("div.quote").remove()
                    val comment = if (post?.isSub == true)
                        "[quote][b]${post.nickname}[/b] 说: ${body.text()}[/quote]\n" else ""
                    showReplyPopupWindow(topic.post, FormBody.Builder()
                            .add("lastview", topic.lastview!!)
                            .add("formhash", topic.formhash!!)
                            .add("topic_id", post.pst_mid)
                            .add("related", post.relate)
                            .add("post_uid", post.pst_uid), comment, "回复 ${post.nickname} 的评论", post.pst_id)
                }
                R.id.item_del -> {
                    AlertDialog.Builder(context).setTitle("确认删除？")
                            .setNegativeButton("取消") { _, _ -> }.setPositiveButton("确定") { _, _ ->
                                if (post.floor == 1) {
                                    val url = topic.post.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase").replace("/new_reply", "?gh=${topic.formhash}&ajax=1")
                                    ApiHelper.buildHttpCall(url) {
                                        true
                                    }.enqueue(ApiHelper.buildCallback<Boolean>(context, {
                                        if (it) context.finish()
                                    }) {})
                                } else {
                                    val url = Bangumi.SERVER + when (post.model) {
                                        "group" -> "/erase/group/reply/" //http://bangumi.tv/group/reply/1365766/edit
                                        "prsn" -> "/erase/reply/person/"
                                        "crt" -> "/erase/reply/character/" //http://bangumi.tv/character/edit_reply/83994
                                        "ep" -> "/erase/reply/ep/" //http://bangumi.tv/subject/ep/edit_reply/641453
                                        "subject" -> "/erase/subject/reply/"//http://bangumi.tv/subject/reply/114260/edit
                                        else -> ""
                                    } + "${post.pst_id}?gh=${topic.formhash}&ajax=1"
                                    ApiHelper.buildHttpCall(url) {
                                        it.body()?.string()?.contains("\"status\":\"ok\"") == true
                                    }.enqueue(ApiHelper.buildCallback<Boolean>(context, {
                                        val data = ArrayList(topicView.adapter.data)
                                        data.removeAll { topicPost -> topicPost.pst_id == post.pst_id }
                                        topicView.setNewData(data)
                                    }) {})
                                }
                            }.show()
                }
                R.id.item_edit -> {
                    val url = if (post.floor == 1)
                        topic.post.replace("/new_reply", "/edit")
                    else Bangumi.SERVER + when (post.model) {
                        "group" -> "/group/reply/${post.pst_id}/edit"
                        "prsn" -> "/person/edit_reply/${post.pst_id}"
                        "crt" -> "/character/edit_reply/${post.pst_id}"
                        "ep" -> "/subject/ep/edit_reply/${post.pst_id}"
                        "subject" -> "/subject/reply/${post.pst_id}/edit"
                        else -> ""
                    }
                    //WebActivity.launchUrl(this@TopicActivity, url)
                    ApiHelper.buildHttpCall(url){
                        val doc = Jsoup.parse(it.body()?.string()?:return@buildHttpCall null)
                        doc.selectFirst("#content")?.text()
                    }.enqueue(ApiHelper.buildCallback(context, {
                        if(it == null){
                            WebActivity.launchUrl(context, url)
                            return@buildCallback
                        }else{
                            buildPopupWindow("修改主题\"${topic.title}\""+if(post.floor == 1) "" else "的回复", it){inputString, send->
                                if(send){
                                    ApiHelper.buildHttpCall(url, body = FormBody.Builder()
                                            .add("formhash", topic.formhash!!)
                                            .add("title", topic.title)
                                            .add("submit", "改好了")
                                            .add("content", inputString).build()){true}.enqueue(ApiHelper.buildCallback(context, {
                                        getTopic(post.pst_id)
                                    })) }
                            }
                        }
                    }) {})
                }
            }
        }
    }

    private fun buildPopupWindow(hint: String = "", draft: String = "", callback: (String, Boolean)->Unit) {
        val dialog = ReplyDialog()
        dialog.hint = hint
        dialog.draft = draft
        dialog.callback = callback
        dialog.show(context.supportFragmentManager, "reply")
    }

    private val drafts = HashMap<String, String>()
    @SuppressLint("InflateParams")
    private fun showReplyPopupWindow(post: String, data: FormBody.Builder, comment: String = "", hint: String = "", draftId: String = "topic") {
        buildPopupWindow(hint, drafts[draftId]?:"") { inputString, send->
            if(send){
                data.add("submit", "submit")
                data.add("content", comment + inputString)
                ApiHelper.buildHttpCall(post, body = data.build()){ response ->
                    val replies = ArrayList(topicView.adapter.data)
                    replies.removeAll { it.sub_floor > 0 }
                    replies.toTypedArray().forEach { replies.addAll(it.subItems?:return@forEach) }
                    val posts = JsonUtil.toJsonObject(response.body()?.string()?:"").getAsJsonObject("posts")
                    val main = JsonUtil.toEntity<Map<String, TopicPost>>(posts.get("main")?.toString()?:"", object: TypeToken<Map<String, TopicPost>>(){}.type)?: HashMap()
                    main.forEach {
                        it.value.floor = (replies.last()?.floor?:0)+1
                        it.value.relate = it.key
                        replies.removeAll { o-> o.pst_id == it.value.pst_id }
                        replies.add(it.value)
                        //adapter.addData(it.value)
                    }
                    val sub = JsonUtil.toEntity<Map<String, TopicActivity.PostList>>(posts.get("sub")?.toString()?:"", object: TypeToken<Map<String, TopicActivity.PostList>>(){}.type)?:HashMap()
                    sub.forEach {
                        var relate = replies.lastOrNull { old-> old.relate == it.key }?:return@forEach
                        it.value.forEach { topicPost ->
                            topicPost.isSub = true
                            topicPost.floor = relate.floor
                            topicPost.sub_floor = relate.sub_floor+1
                            topicPost.editable = topicPost.is_self
                            topicPost.relate = relate.relate
                            replies.removeAll { o-> o.pst_id == topicPost.pst_id }
                            replies.add(topicPost)
                            relate = topicPost
                        }
                    }
                    replies.sortedBy { it.floor + it.sub_floor * 1.0f/replies.size }
                }.enqueue(ApiHelper.buildCallback<List<TopicPost>>(context, {
                    topicView.setNewData(it)
                }) {})
            }
            else{
                drafts[draftId] = inputString
            }
        }
    }

}