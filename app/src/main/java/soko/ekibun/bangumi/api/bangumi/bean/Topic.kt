package soko.ekibun.bangumi.api.bangumi.bean

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Response
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.util.concurrent.ConcurrentLinkedQueue

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
        suspend fun getList(
            type: String
        ): List<Topic> {
            return withContext(Dispatchers.IO) {
                val doc = Jsoup.parse(
                    HttpUtil.fetch(
                        "${Bangumi.SERVER}/rakuen/topiclist" + if (type.isEmpty()) "" else "?type=$type"
                    ).body?.string() ?: ""
                )
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

        private fun parsePost(str: String): List<TopicPost> {
            val doc = Jsoup.parse(str)
            doc.outputSettings().prettyPrint(false)
            var relate: TopicPost? = null
            return doc.select(".re_info").mapNotNull {
                val post = TopicPost.parse(it.parent())
                when {
                    post == null -> null
                    post.isSub -> {
                        relate?.children?.add(post)
                        null
                    }
                    else -> {
                        relate = post
                        post
                    }
                }
            }
        }

        /**
         * 加载帖子（Sax）
         * @param topic Topic
         * @return Observable<Boolean|List<TopicPost>>
         */
        suspend fun getTopicSax(
            topic: Topic,
            onHeader: () -> Unit,
            onTopicPost: (List<TopicPost>) -> Unit
        ) {
            withContext(Dispatchers.IO) {
                val rsp = HttpUtil.fetch(
                    when (topic.model) {
                        "blog" -> "${Bangumi.SERVER}/blog/${topic.id}"
                        else -> "${Bangumi.SERVER}/rakuen/topic/${topic.model}/${topic.id}"
                    }
                )
                var beginString = ""
                var isReplyBox = false;
                var replyCount = -2
                val posts = ConcurrentLinkedQueue<TopicPost>()
                var finishFirst = false
                val endString = ApiHelper.parseSaxAsync(rsp, { tag, attrs ->
                    when {
                        attrs.contains("row_reply") || attrs.contains("postTopic") -> {
                            replyCount++
                            if (replyCount <= 0 || replyCount > 25) {
                                replyCount.also { if (replyCount > 0) replyCount = 0 } to ApiHelper.SaxEventType.BEGIN
                            } else null to ApiHelper.SaxEventType.NOTHING
                        }
                        attrs.contains("reply_wrapper") -> {
                            replyCount++
                            isReplyBox = true
                            replyCount to ApiHelper.SaxEventType.BEGIN
                        }
                        else -> null to ApiHelper.SaxEventType.NOTHING
                    }
                }) { tag, str ->
                    if (tag is Int && tag < 0) {
                        val doc = Jsoup.parseBodyFragment(str)
                        doc.outputSettings().prettyPrint(false)

                        val title = doc.selectFirst("#pageHeader h1")?.ownText()
                        if (title != null) {
                            topic.title = title

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
                                    model = "blog"
                                )
                            }
                            withContext(Dispatchers.Main) { onHeader() }
                        }
                    } else {
                        if(beginString == "" && isReplyBox) {
                            beginString = str
                        }
                        val post = parsePost(str)
                        posts.addAll(post)
                        while (tag is Int && tag != 0 && !finishFirst) delay(100)
                        withContext(Dispatchers.Main) { onTopicPost(post) }
                        if (tag == 0) finishFirst = true
                    }
                }
                val post = parsePost(endString)
                posts.addAll(post)

                val doc = Jsoup.parse(beginString + endString)
                val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                val form = doc.selectFirst("#ReplyForm")
                topic.lastview = form?.selectFirst("input[name=lastview]")?.attr("value")
                topic.error = error?.text()?.let {
                    Pair(it, Bangumi.parseUrl(error.selectFirst("a")?.attr("href") ?: ""))
                }
                topic.replies = posts.sortedBy { it.floor }
            }
        }

        /**
         * 删除帖子
         * @param topic Topic
         * @return Call<Boolean>
         */
        suspend fun remove(
            topic: Topic
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    when (topic.model) {
                        "blog" -> "${Bangumi.SERVER}/erase/entry/${topic.id}"
                        else -> topic.url.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase")
                    } + "?gh=${HttpUtil.formhash}&ajax=1"
                )
            }
        }


        data class ReplyData(
            val posts: ReplyPost
        ) {
            data class ReplyPost(
                val main: Map<String, TopicPost>?,
                val sub: Map<String, List<TopicPost>>?
            )
        }

        /**
         * 回复帖子
         * @param topic Topic
         * @param post TopicPost?
         * @param content String
         * @return Call<List<TopicPost>>
         */
        suspend fun reply(
            topic: Topic,
            post: TopicPost?,
            content: String
        ): ReplyData.ReplyPost {
            return withContext(Dispatchers.IO) {
                val comment = if (post?.isSub == true)
                    "[quote][b]${post.nickname}[/b] 说: ${Jsoup.parse(post.pst_content).let { doc ->
                        doc.select("div.quote").remove()
                        Jsoup.parse(doc.html()).text().let {
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
                JsonUtil.toEntity<ReplyData>(
                    HttpUtil.fetch(
                        when (topic.model) {
                            "blog" -> "${Bangumi.SERVER}/blog/entry/${topic.id}"
                            else -> topic.url
                        } + "/new_reply?ajax=1", HttpUtil.RequestOption(body = data.build())
                    ).body?.string() ?: ""
                )?.posts!!
            }
        }

        /**
         * 编辑帖子
         * @param topic Topic
         * @param title String
         * @param content String
         * @return Call<Boolean>
         */
        suspend fun edit(
            topic: Topic,
            title: String,
            content: String
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    topic.url + "/edit?ajax=1", HttpUtil.RequestOption(
                        body = FormBody.Builder()
                            .add("formhash", HttpUtil.formhash)
                            .add("title", title)
                            .add("submit", "改好了")
                            .add("content", content).build()
                    )
                )
            }
        }
    }
}