package soko.ekibun.bangumi.api.bangumi.bean

import com.google.gson.reflect.TypeToken
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.TextUtil
import java.util.*

/**
 * 帖子
 */
data class Topic(
        val group: String,
        val title: String,
        val images: Images,
        val replies: List<TopicPost>,
        val post: String,
        val lastview: String?,
        val links: Map<String, String>,
        val error: String?,
        val errorLink: String?
) {
    companion object {
        /**
         * 加载帖子（Sax）
         */
        fun getTopicSax(url: String, onBeforePost: (data: String) -> Unit, onNewPost: (post: TopicPost) -> Unit): Call<Topic> {
            return ApiHelper.buildHttpCall(url) { rsp ->
                var beforeData = ""
                val replies = ArrayList<TopicPost>()
                val updateReply = { str: String ->
                    val it = Jsoup.parse(str)
                    it.outputSettings().prettyPrint(false)

                    TopicPost.parse(it)?.let {
                        replies += it
                        onNewPost(it)
                    }
                }
                val lastData = ApiHelper.parseWithSax(rsp) { parser, str ->
                    when {
                        parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                        parser.getAttributeValue("", "id")?.startsWith("post_") == true -> {
                            if (beforeData.isEmpty()) {
                                beforeData = str
                                onBeforePost(str)
                            } else {
                                updateReply(str)
                            }
                            ApiHelper.SaxEventType.BEGIN
                        }
                        parser.getAttributeValue("", "id")?.contains("reply_wrapper") == true -> {
                            updateReply(str)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        else -> ApiHelper.SaxEventType.NOTHING
                    }
                }

                val doc = Jsoup.parse(beforeData + lastData)
                val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                val form = doc.selectFirst("#ReplyForm")
                Topic(
                        group = doc.selectFirst("#pageHeader span")?.text() ?: "",
                        title = doc.selectFirst("#pageHeader h1")?.ownText() ?: "",
                        images = Images(Bangumi.parseImageUrl(doc.selectFirst("#pageHeader img"))),
                        replies = replies,
                        post = Bangumi.parseUrl("${form?.attr("action")}?ajax=1"),
                        lastview = form?.selectFirst("input[name=lastview]")?.attr("value"),
                        links = LinkedHashMap<String, String>().let { links ->
                            doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                                links[it.text()] = Bangumi.parseUrl(it.attr("href") ?: "")
                            }
                            links
                        },
                        error = error?.text(),
                        errorLink = Bangumi.parseUrl(error?.selectFirst("a")?.attr("href") ?: ""))
            }
        }

        /**
         * 加载帖子
         */
        fun getTopic(url: String): Call<Topic> {
            return ApiHelper.buildHttpCall(url) { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.outputSettings().prettyPrint(false)
                val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                val form = doc.selectFirst("#ReplyForm")
                HttpUtil.formhash = doc.selectFirst("input[name=formhash]")?.attr("value") ?: HttpUtil.formhash
                Topic(
                        group = doc.selectFirst("#pageHeader span")?.text() ?: "",
                        title = doc.selectFirst("#pageHeader h1")?.ownText() ?: "",
                        images = Images(Bangumi.parseImageUrl(doc.selectFirst("#pageHeader img"))),
                        replies = doc.select("div[id^=post_]")?.mapNotNull {
                            TopicPost.parse(it)
                        } ?: ArrayList(),
                        post = Bangumi.parseUrl("${form?.attr("action")}?ajax=1"),
                        lastview = form?.selectFirst("input[name=lastview]")?.attr("value"),
                        links = LinkedHashMap<String, String>().let { links ->
                            doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                                links[it.text()] = Bangumi.parseUrl(it.attr("href") ?: "")
                            }
                            links
                        },
                        error = error?.text(),
                        errorLink = Bangumi.parseUrl(error?.selectFirst("a")?.attr("href") ?: ""))
            }
        }

        /**
         * 删除帖子
         * TODO 时间线
         */
        fun remove(
                topic: Topic
        ): Call<Boolean> {
            return ApiHelper.buildHttpCall(topic.post.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase").replace("/new_reply", "?gh=${HttpUtil.formhash}&ajax=1")) {
                it.code == 200
            }
        }

        /**
         * 回复帖子
         * TODO 时间线
         */
        fun reply(
                topic: Topic,
                post: TopicPost?,
                content: String
        ): Call<List<TopicPost>> {
            val comment = if (post?.isSub == true)
                "[quote][b]${post.nickname}[/b] 说: ${Jsoup.parse(post.pst_content).let { doc ->
                    doc.select("div.quote").remove()
                    TextUtil.html2text(doc.html()).let {
                        if (it.length > 100) it.substring(0, 100) + "..." else it
                    }
                }}[/quote]\n" else ""

            val data = FormBody.Builder()
                    .add("lastview", topic.lastview!!)
                    .add("formhash", HttpUtil.formhash)
                    .add("content", comment + content)
                    .add("submit", "submit")
            if (post != null) {
                data.add("topic_id", post.pst_mid)
                        .add("related", post.relate)
                        .add("post_uid", post.pst_uid)
            }

            return ApiHelper.buildHttpCall(topic.post, body = data.build()) { rsp ->
                val replies = ArrayList(topic.replies)
                replies.removeAll { it.sub_floor > 0 }
                replies.sortedBy { it.floor }
                val posts = JsonUtil.toJsonObject(rsp.body?.string() ?: "").getAsJsonObject("posts")
                val main = JsonUtil.toEntity<Map<String, TopicPost>>(posts.get("main")?.toString()
                        ?: "", object : TypeToken<Map<String, TopicPost>>() {}.type) ?: HashMap()
                main.forEach {
                    it.value.floor = (replies.last()?.floor ?: 0) + 1
                    it.value.relate = it.key
                    it.value.isExpanded = replies.firstOrNull { o -> o.pst_id == it.value.pst_id }?.isExpanded ?: true
                    replies.removeAll { o -> o.pst_id == it.value.pst_id }
                    replies.add(it.value)
                }
                replies.toTypedArray().forEach { replies.addAll(it.subItems ?: return@forEach) }
                replies.sortedBy { it.floor + it.sub_floor * 1.0f / replies.size }
                val sub = JsonUtil.toEntity<Map<String, List<TopicPost>>>(posts.get("sub")?.toString()
                        ?: "", object : TypeToken<Map<String, List<TopicPost>>>() {}.type) ?: HashMap()
                sub.forEach {
                    replies.lastOrNull { old -> old.pst_id == it.key }?.isExpanded = true
                    var relate = replies.lastOrNull { old -> old.relate == it.key } ?: return@forEach
                    it.value.forEach { topicPost ->
                        topicPost.isSub = true
                        topicPost.floor = relate.floor
                        topicPost.sub_floor = relate.sub_floor + 1
                        topicPost.editable = topicPost.is_self
                        topicPost.relate = relate.relate
                        replies.removeAll { o -> o.pst_id == topicPost.pst_id }
                        replies.add(topicPost)
                        relate = topicPost
                    }
                }
                replies.sortedBy { it.floor + it.sub_floor * 1.0f / replies.size }
            }
        }
    }
}