package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import android.webkit.CookieManager
import com.google.gson.reflect.TypeToken
import okhttp3.FormBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.JsonUtil
import android.support.v7.widget.RecyclerView
import android.view.View


class TopicActivity : AppCompatActivity() {

    val adapter = PostAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic)

        item_list.adapter = adapter
        item_list.layoutManager = object: LinearLayoutManager(this){
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean): Boolean { return false }
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean, focusedChildVisible: Boolean): Boolean { return false }
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        item_swipe.setOnRefreshListener {
            getTopic()
        }
        getTopic()
    }

    override fun onStart() {
        super.onStart()
        BackgroundWebView(this).loadUrl(Bangumi.SERVER)
    }

    @SuppressLint("InflateParams")
    private fun showPopupWindow(post: String, data: FormBody.Builder, comment: String = "", hint: String = "") {
        val dialog = ReplyDialog()
        dialog.hint = hint
        dialog.callback = { inputString->
            data.add("submit", "submit")
            data.add("content", comment + inputString)
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            ApiHelper.buildHttpCall(post, mapOf("cookie" to cookie), data.build()){
                val replies = ArrayList(adapter.data)
                val posts = JsonUtil.toJsonObject(it.body()?.string()?:"").getAsJsonObject("posts")
                val main = JsonUtil.toEntity<Map<String, TopicPost>>(posts.get("main")?.toString()?:"", object: TypeToken<Map<String, TopicPost>>(){}.type)?: HashMap()
                main.forEach {
                    it.value.floor = (replies.last()?.floor?:0)+1
                    it.value.relate = it.key
                    replies.removeAll { o-> o.pst_id == it.value.pst_id }
                    replies.add(it.value)
                    //adapter.addData(it.value)
                }
                val sub = JsonUtil.toEntity<Map<String, PostList>>(posts.get("sub")?.toString()?:"", object: TypeToken<Map<String, PostList>>(){}.type)?:HashMap()
                sub.forEach {
                    var relate = replies.lastOrNull { old-> old.relate == it.key }?:return@forEach
                    it.value.forEach {
                        it.isSub = true
                        it.floor = relate.floor
                        it.sub_floor = relate.sub_floor+1
                        it.editable = it.is_self
                        it.relate = relate.relate
                        replies.removeAll { o-> o.pst_id == it.pst_id }
                        replies.add(it)
                        relate = it
                    }
                }
                replies.sortedBy { it.floor + it.sub_floor * 1.0f/replies.size }
            }.enqueue(ApiHelper.buildCallback<List<TopicPost>>(this@TopicActivity, {
                setNewData(it)
            }) {})
        }
        dialog.show(supportFragmentManager, "reply")
    }

    fun setNewData(data: List<TopicPost>){
        var floor = 0
        var subFloor = 0
        var referPost: TopicPost? = null
        data.forEach {
            if(it.isSub){
                subFloor++
            }else{
                floor++
                subFloor=0 }
            it.floor = floor
            it.sub_floor = subFloor
            it.editable = it.is_self
            if(subFloor == 0) referPost = it
            else referPost?.editable = false
        }
        adapter.setNewData(data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getTopic(){
        item_swipe.isRefreshing = true
        val openUrl = intent.getStringExtra(EXTRA_TOPIC)
        Bangumi.getTopic(openUrl).enqueue(ApiHelper.buildCallback(this, {topic->
            title = topic.title
            toolbar.subtitle = topic.group
            if(topic.replies.isEmpty())
                finish()
            toolbar.menu.clear()
            topic.links.forEach {
                toolbar.menu.add(it.key)
            }
            toolbar.setOnMenuItemClickListener {
                WebActivity.launchUrl(this@TopicActivity, topic.links[it.title]?:"", openUrl)
                true
            }
            setNewData(topic.replies)
            adapter.setOnLoadMoreListener({adapter.loadMoreEnd()}, item_list)
            adapter.setEnableLoadMore(true)
            item_reply.setOnClickListener {
                topic.formhash?.let{ showPopupWindow(topic.post, FormBody.Builder().add("lastview", topic.lastview!!).add("formhash", it),"", "回复 ${topic.title}") }
            }
            adapter.setOnItemChildClickListener { _, v, position ->
                val post = adapter.data[position]
                when(v.id){
                    R.id.item_avatar->
                        WebActivity.launchUrl(v.context, "${Bangumi.SERVER}/user/${post.username}")
                    R.id.item_reply->{
                        val body = Jsoup.parse(post.pst_content).body()
                        body.select("div.quote").remove()
                        val comment = if(post?.isSub == true)
                            "[quote][b]${post.nickname}[/b] 说: ${body.text()}[/quote]\n" else ""
                        showPopupWindow(topic.post, FormBody.Builder()
                                .add("lastview", topic.lastview!!)
                                .add("formhash", topic.formhash!!)
                                .add("topic_id" ,post.pst_mid)
                                .add("related", post.relate)
                                .add("post_uid", post.pst_uid), comment, "回复 ${post.nickname} 的评论")
                    }
                    R.id.item_del-> {
                        AlertDialog.Builder(this@TopicActivity).setTitle("确认删除？")
                                .setNegativeButton("取消", { _, _ -> }).setPositiveButton("确定") { _, _ ->
                                    if (post.floor == 1) {
                                        val url = topic.post.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase").replace("/new_reply", "?gh=${topic.formhash}&ajax=1")
                                        val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER) ?: ""
                                        ApiHelper.buildHttpCall(url, mapOf("cookie" to cookie)) {
                                            true
                                        }.enqueue(ApiHelper.buildCallback<Boolean>(this@TopicActivity, {
                                            if (it) finish()
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
                                        val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER) ?: ""
                                        ApiHelper.buildHttpCall(url, mapOf("cookie" to cookie)) {
                                            it.body()?.string()?.contains("\"status\":\"ok\"") == true
                                        }.enqueue(ApiHelper.buildCallback<Boolean>(this@TopicActivity, {
                                            val data = ArrayList(adapter.data)
                                            data.removeAll { it.pst_id == post.pst_id }
                                            setNewData(data)
                                        }) {})
                                    } }.show()
                    }
                    R.id.item_edit->{
                        val url = if(post.floor == 1)
                            topic.post.replace("/new_reply", "/edit")
                        else  Bangumi.SERVER +  when(post.model){
                            "group" -> "/group/reply/${post.pst_id}/edit"
                            "prsn" -> "/person/edit_reply/${post.pst_id}"
                            "crt" -> "/character/edit_reply/${post.pst_id}"
                            "ep" -> "/subject/ep/edit_reply/${post.pst_id}"
                            "subject" -> "/subject/reply/${post.pst_id}/edit"
                            else -> "" }
                        WebActivity.launchUrl(this@TopicActivity, url)
                    }
                }
            }
        }){item_swipe.isRefreshing = false})
    }

    class PostList: ArrayList<TopicPost>()

    companion object{
        private const val EXTRA_TOPIC = "extraTopic"

        fun startActivity(context: Context, topic: String) {
            context.startActivity(parseIntent(context, topic))
        }

        private fun parseIntent(context: Context, topic: String): Intent {
            val intent = Intent(context.applicationContext, TopicActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_TOPIC, topic)
            return intent
        }
    }
}
