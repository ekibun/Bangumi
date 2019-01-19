package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
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
import android.view.Menu
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.ResourceUtil
import java.net.URI


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

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ appBarLayout, verticalOffset ->
            val ratio = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            title_collapse.alpha = 1-(1-ratio)*(1-ratio)*(1-ratio)
            title_expand.alpha = 1-ratio
            title_collapse.translationY = -title_slice.height / 2 * ratio
            title_expand.translationY = title_collapse.translationY
            title_slice.translationY =  (title_collapse.height - title_expand.height - (title_slice.layoutParams as ConstraintLayout.LayoutParams).topMargin - title_slice.height / 2) * ratio
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    @SuppressLint("InflateParams")
    private fun showPopupWindow(post: String, data: FormBody.Builder, comment: String = "", hint: String = "") {
        val dialog = ReplyDialog()
        dialog.hint = hint
        dialog.callback = { inputString->
            data.add("submit", "submit")
            data.add("content", comment + inputString)
            ApiHelper.buildHttpCall(post, mapOf("cookie" to (CookieManager.getInstance().getCookie(Bangumi.SERVER)?:"")), data.build()){
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

    private fun setNewData(data: List<TopicPost>){
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_share -> AppUtil.shareString(this, "${title_expand.text} $openUrl")
            R.id.action_refresh -> getTopic()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getTopic(){
        item_swipe.isRefreshing = true
        getTopicApi{
            if(it.user_id.isNullOrEmpty() && UserModel(this).getToken() != null){
                item_swipe.isRefreshing = true
                val webView = BackgroundWebView(this)
                webView.loadUrl("${Bangumi.SERVER}/m")
                webView.onPageFinished = {
                    getTopicApi{
                        processTopic(it)
                    }
                    webView.onPageFinished = {}
                }
            }else processTopic(it)
        }
    }

    private fun processTopic(topic: Topic){
        title = ""//topic.title
        title_collapse.text = topic.title
        title_expand.text = title_collapse.text
        title_collapse.setOnClickListener {
            WebActivity.launchUrl(this@TopicActivity, topic.links.toList().last().second, openUrl)
        }
        title_expand.setOnClickListener {
            WebActivity.launchUrl(this@TopicActivity, topic.links.toList().last().second, openUrl)
        }
        topic.links.toList().getOrNull(0)?.let{link ->
            title_slice_0.text = link.first
            title_slice_0.setOnClickListener {
                WebActivity.launchUrl(this@TopicActivity, link.second, openUrl)
            }
        }
        topic.links.toList().getOrNull(1)?.let{link ->
            title_slice_1.text = link.first
            title_slice_1.setOnClickListener {
                WebActivity.launchUrl(this@TopicActivity, link.second, openUrl)
            }
        }
        title_slice_divider.visibility = if(topic.links.size>2) View.VISIBLE else View.GONE
        title_slice_1.visibility = title_slice_divider.visibility
        title_slice_0.post{
            title_slice_0.maxWidth = title_expand.width - if(title_slice_divider.visibility == View.VISIBLE) 2*title_slice_divider.width + title_slice_1.width else 0
        }

        //toolbar.subtitle = topic.group
        if(topic.replies.isEmpty())
            finish()
        Glide.with(item_cover_blur)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(item_cover_blur.drawable))
                .load(HttpUtil.getUrl(topic.replies.firstOrNull()?.avatar?:"", URI.create(Bangumi.SERVER)))
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(item_cover_blur)
        setNewData(topic.replies)
        (item_list?.layoutManager as? LinearLayoutManager)?.let{ it.scrollToPositionWithOffset(adapter.data.indexOfFirst { it.pst_id == openPost.toString() }, 0) }
        adapter.setOnLoadMoreListener({adapter.loadMoreEnd()}, item_list)
        adapter.setEnableLoadMore(true)
        item_reply.text = when {
            !topic.formhash.isNullOrEmpty() -> getString(R.string.reply_hint)
            !topic.error.isNullOrEmpty() -> topic.error
            else -> "登录后才可以评论哦"
        }
        item_reply.setCompoundDrawablesWithIntrinsicBounds(
                if(!topic.formhash.isNullOrEmpty())ResourceUtil.getDrawable(this, R.drawable.ic_edit) else null,//left
                null,
                if(!topic.formhash.isNullOrEmpty())ResourceUtil.getDrawable(this, R.drawable.ic_send) else null,//right
                null)
        item_reply.setOnClickListener {
            topic.formhash?.let{ showPopupWindow(topic.post, FormBody.Builder().add("lastview", topic.lastview!!).add("formhash", it),"", "回复 ${topic.title}") }?:{
                if(!topic.errorLink.isNullOrEmpty()) WebActivity.launchUrl(this, topic.errorLink, "")
            }()
        }
        adapter.setOnItemChildClickListener { _, v, position ->
            val post = adapter.data[position]
            when (v.id) {
                R.id.item_avatar ->
                    WebActivity.launchUrl(v.context, "${Bangumi.SERVER}/user/${post.username}")
                R.id.item_reply -> {
                    val body = Jsoup.parse(post.pst_content).body()
                    body.select("div.quote").remove()
                    val comment = if (post?.isSub == true)
                        "[quote][b]${post.nickname}[/b] 说: ${body.text()}[/quote]\n" else ""
                    showPopupWindow(topic.post, FormBody.Builder()
                            .add("lastview", topic.lastview!!)
                            .add("formhash", topic.formhash!!)
                            .add("topic_id", post.pst_mid)
                            .add("related", post.relate)
                            .add("post_uid", post.pst_uid), comment, "回复 ${post.nickname} 的评论")
                }
                R.id.item_del -> {
                    AlertDialog.Builder(this@TopicActivity).setTitle("确认删除？")
                            .setNegativeButton("取消") { _, _ -> }.setPositiveButton("确定") { _, _ ->
                                if (post.floor == 1) {
                                    val url = topic.post.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase").replace("/new_reply", "?gh=${topic.formhash}&ajax=1")
                                    ApiHelper.buildHttpCall(url, mapOf("cookie" to (CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""))) {
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
                                    ApiHelper.buildHttpCall(url, mapOf("cookie" to (CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""))) {
                                        it.body()?.string()?.contains("\"status\":\"ok\"") == true
                                    }.enqueue(ApiHelper.buildCallback<Boolean>(this@TopicActivity, {
                                        val data = ArrayList(adapter.data)
                                        data.removeAll { it.pst_id == post.pst_id }
                                        setNewData(data)
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
                    WebActivity.launchUrl(this@TopicActivity, url)
                }
            }
        }
    }

    val openUrl by lazy{ intent.getStringExtra(EXTRA_TOPIC) }
    val openPost by lazy{ intent.getIntExtra(EXTRA_POST, 0) }
    private fun getTopicApi(callback: (Topic)->Unit){
        Bangumi.getTopic(openUrl).enqueue(ApiHelper.buildCallback(this, {topic->
            callback(topic)
        }){item_swipe.isRefreshing = false})
    }

    class PostList: ArrayList<TopicPost>()

    companion object{
        private const val EXTRA_TOPIC = "extraTopic"
        private const val EXTRA_POST = "extraPost"

        fun startActivity(context: Context, topic: String, post: Int = 0) {
            context.startActivity(parseIntent(context, topic, post))
        }

        private fun parseIntent(context: Context, topic: String, post: Int): Intent {
            val intent = Intent(context.applicationContext, TopicActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_TOPIC, topic)
            intent.putExtra(EXTRA_POST, post)
            return intent
        }
    }
}
