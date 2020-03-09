package soko.ekibun.bangumi.api.bangumi.bean

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
 * @property model String
 * @property id Int
 * @property title String?
 * @property links Map<String, String>?
 * @property image String?
 * @property replies List<TopicPost>
 * @property lastview String?
 * @property error Pair<String, String>?
 * @property replyCount Int
 * @property time String?
 * @property blog TopicPost?
 * @property user UserInfo?
 * @property cacheKey String
 * @property url String
 * @constructor
 */
data class Topic(
    val model: String,
    val id: Int,
    var title: String? = null,
    var links: Map<String, String>? = null,
    var image: String? = null,
    var replies: List<TopicPost> = ArrayList(),
    var lastview: String? = null,
    var error: Pair<String, String>? = null,
    var replyCount: Int = 0,
    var time: String? = null,
    var blog: TopicPost? = null,
    var user: UserInfo? = null // blog & subject
) {
    val cacheKey = "topic_${model}_$id"

    val url = "${Bangumi.SERVER}/${when (model) {
        "group" -> "group/topic"
        "ep" -> "subject/ep"
        "subject" -> "subject/topic"
        "crt" -> "character"
        "prsn" -> "person"
        "blog" -> "blog"
        else -> ""
    }}/$id"

    companion object {
        /**
         * 超展开
         * @param type String
         * @return Call<List<Topic>>
         */
        fun getList(
            type: String
        ): Call<List<Topic>> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/rakuen/topiclist" + if (type.isEmpty()) "" else "?type=$type") { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.select(".item_list").mapNotNull {
                    val title = it.selectFirst(".title") ?: return@mapNotNull null
                    val modelId = Regex("""/rakuen/topic/([^/]+)/(\d+)""").find(title.attr("href") ?: "")?.groupValues
                        ?: return@mapNotNull null

                    val group = it.selectFirst(".row").selectFirst("a")
                    Topic(
                        model = modelId[1],
                        id = modelId[2].toInt(),
                        image = Bangumi.parseImageUrl(it.selectFirst("span.avatarNeue")),
                        title = title.text(),
                        links = mapOf((group?.text() ?: "") to Bangumi.parseUrl(group?.attr("href") ?: "")),
                        time = it.selectFirst(".time")?.text()?.replace("...", "") ?: "",
                        replyCount = it.selectFirst(".grey")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0
                    )
                }
            }
        }

        /**
         * 加载帖子（Sax）
         * @param topic Topic
         * @param onUpdate Function1<Topic, Unit>
         * @param onNewPost Function1<[@kotlin.ParameterName] TopicPost, Unit>
         * @return Call<Topic>
         */
        fun getTopicSax(topic: Topic, onUpdate: (Topic) -> Unit, onNewPost: (post: TopicPost) -> Unit): Call<Topic> {
            return ApiHelper.buildHttpCall(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/blog/${topic.id}"
                    else -> "${Bangumi.SERVER}/rakuen/topic/${topic.model}/${topic.id}"
                }
            ) { rsp ->
                var beforeData = ""
                val replies = ArrayList<TopicPost>()
                val updateReply = { str: String ->
                    val doc = Jsoup.parse(str)
                    doc.outputSettings().prettyPrint(false)
                    if (beforeData.isEmpty()) {
                        beforeData = str
                        topic.title = doc.selectFirst("#pageHeader h1")?.ownText()

                        topic.links = LinkedHashMap<String, String>().let { links ->
                            doc.select("#pageHeader a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                                links[it.text()] = Bangumi.parseUrl(it.attr("href") ?: "")
                            }
                            links
                        }

                        val image = Bangumi.parseImageUrl(doc.selectFirst("#pageHeader img"))
                        topic.image = image
                        if (topic.model == "blog") {
                            val userInfo = doc.selectFirst("#pageHeader a.avatar")
                            val userName = UserInfo.getUserName(userInfo?.attr("href")) ?: ""
                            topic.blog = TopicPost(
                                "", "",
                                pst_uid = userName,
                                username = userName,
                                nickname = userInfo?.text() ?: "",
                                avatar = image,
                                pst_content = doc.selectFirst("#entry_content")?.html() ?: "",
                                dateline = doc.selectFirst(".re_info")?.text()?.substringBefore('/')?.trim(' ')
                                    ?: "",
                                is_self = doc.selectFirst(".re_info")?.text()?.contains("del") == true,
                                editable = doc.selectFirst(".re_info")?.text()?.contains("del") == true,
                                model = "blog"
                            )
                            onNewPost(topic.blog!!)
                        }
                        onUpdate(topic)
                    } else {
                        TopicPost.parse(doc)?.let {
                            replies += it
                            onNewPost(it)
                        }
                    }
                }
                val lastData = ApiHelper.parseWithSax(rsp) { parser, str ->
                    when {
                        parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                        parser.getAttributeValue("", "id")?.startsWith("post_") == true -> {
                            updateReply(str)
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
                topic.lastview = form?.selectFirst("input[name=lastview]")?.attr("value")
                topic.error = error?.text()?.let {
                    Pair(it, Bangumi.parseUrl(error.selectFirst("a")?.attr("href") ?: ""))
                }
                topic.replies = replies
                topic
            }
        }

        /**
         * 删除帖子
         * @param topic Topic
         * @return Call<Boolean>
         */
        fun remove(
            topic: Topic
        ): Call<Boolean> {
            return ApiHelper.buildHttpCall(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/erase/entry/${topic.id}"
                    else -> topic.url.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase")
                } + "?gh=${HttpUtil.formhash}&ajax=1"
            ) {
                it.code == 200
            }
        }

        /**
         * 回复帖子
         * @param topic Topic
         * @param post TopicPost?
         * @param content String
         * @return Call<List<TopicPost>>
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
                .add("lastview", topic.lastview ?: "")
                .add("formhash", HttpUtil.formhash)
                .add("content", comment + content)
                .add("submit", "submit")
            if (post != null) {
                data.add("topic_id", post.pst_mid)
                    .add("related", post.relate)
                    .add("post_uid", post.pst_uid)
            }

            return ApiHelper.buildHttpCall(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/blog/entry/${topic.id}"
                    else -> topic.url
                } + "/new_reply?ajax=1", body = data.build()
            ) { rsp ->
                val replies = ArrayList(topic.replies)
                replies.removeAll { it.sub_floor > 0 }
                replies.sortedBy { it.floor }
                val posts = JsonUtil.toJsonObject(rsp.body?.string() ?: "").getAsJsonObject("posts")
                val main = JsonUtil.toEntity<Map<String, TopicPost>>(posts.get("main")?.toString() ?: "") ?: HashMap()
                main.forEach {
                    it.value.floor = (replies.lastOrNull()?.floor ?: 0) + 1
                    it.value.relate = it.key
                    it.value.isExpanded = replies.firstOrNull { o -> o.pst_id == it.value.pst_id }?.isExpanded ?: true
                    replies.removeAll { o -> o.pst_id == it.value.pst_id }
                    replies.add(it.value)
                }
                replies.toTypedArray().forEach { replies.addAll(it.subItems ?: return@forEach) }
                replies.sortedBy { it.floor + it.sub_floor * 1.0f / replies.size }
                val sub = JsonUtil.toEntity<Map<String, List<TopicPost>>>(posts.get("sub")?.toString() ?: "")
                    ?: HashMap()
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

        /**
         * 编辑帖子
         * @param topic Topic
         * @param title String
         * @param content String
         * @return Call<Boolean>
         */
        fun edit(
            topic: Topic,
            title: String,
            content: String
        ): Call<Boolean> {
            return ApiHelper.buildHttpCall(
                topic.url + "/edit?ajax=1", body = FormBody.Builder()
                    .add("formhash", HttpUtil.formhash)
                    .add("title", title)
                    .add("submit", "改好了")
                    .add("content", content).build()
            ) {
                it.code == 200
            }
        }
    }
}