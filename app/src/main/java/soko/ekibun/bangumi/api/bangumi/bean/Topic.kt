package soko.ekibun.bangumi.api.bangumi.bean

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

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
        ): Observable<List<Topic>> {
            return ApiHelper.createHttpObservable(
                "${Bangumi.SERVER}/rakuen/topiclist" + if (type.isEmpty()) "" else "?type=$type"
            ).map { rsp ->
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
        fun getTopicSax(topic: Topic): Observable<Any> {
            return ApiHelper.createHttpObservable(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/blog/${topic.id}"
                    else -> "${Bangumi.SERVER}/rakuen/topic/${topic.model}/${topic.id}"
                }
            ).flatMap { rsp ->
                Observable.create<Any> { emitter ->
                    var beforeData = ""

                    val replyPub = ReplaySubject.create<String>()
                    val observable = replyPub.subscribeOn(Schedulers.newThread()).buffer(20).flatMap { str ->
                        Observable.just(0).observeOn(Schedulers.computation()).takeWhile {
                            !emitter.isDisposed
                        }.map {
                            parsePost(str.joinToString(""))
                        }
                    }

                    observable.subscribe({
                        if (!emitter.isDisposed) emitter.onNext(it)
                    }, {
                        if (!emitter.isDisposed) emitter.onError(it)
                    })

                    val firstReplies = ArrayList<TopicPost>()
                    val updateReply = { str: String ->
                        if (beforeData.isEmpty()) {
                            beforeData = str
                            val doc = Jsoup.parse(str)
                            doc.outputSettings().prettyPrint(false)

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
                                    model = "blog"
                                )
                            }
                            if (!emitter.isDisposed) emitter.onNext(true)
                        } else if (firstReplies.isEmpty()) {
                            val posts = parsePost(str)
                            firstReplies.addAll(posts)
                            emitter.onNext(posts)
                        } else {
                            replyPub.onNext(str)
                        }
                    }
                    val lastData = ApiHelper.parseWithSax(rsp) { parser, str ->
                        when {
                            parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                            parser.getAttributeValue("", "class")?.contains(Regex("row_reply|postTopic")) == true -> {
                                updateReply(str())
                                ApiHelper.SaxEventType.BEGIN
                            }
                            parser.getAttributeValue("", "id")?.contains("reply_wrapper") == true -> {
                                updateReply(str())
                                ApiHelper.SaxEventType.BEGIN
                            }
                            else -> ApiHelper.SaxEventType.NOTHING
                        }
                    }
                    replyPub.onComplete()

                    val doc = Jsoup.parse(beforeData + lastData)
                    val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                    val form = doc.selectFirst("#ReplyForm")
                    topic.lastview = form?.selectFirst("input[name=lastview]")?.attr("value")
                    topic.error = error?.text()?.let {
                        Pair(it, Bangumi.parseUrl(error.selectFirst("a")?.attr("href") ?: ""))
                    }
                    topic.replies = firstReplies.plus(observable.blockingIterable().flatten()).sortedBy { it.floor }
                    if (!emitter.isDisposed) {
                        emitter.onNext(false)
                        emitter.onComplete()
                    }
                }
            }
        }

        /**
         * 删除帖子
         * @param topic Topic
         * @return Call<Boolean>
         */
        fun remove(
            topic: Topic
        ): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/erase/entry/${topic.id}"
                    else -> topic.url.replace(Bangumi.SERVER, "${Bangumi.SERVER}/erase")
                } + "?gh=${HttpUtil.formhash}&ajax=1"
            ).map { rsp ->
                rsp.code == 200
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
        fun reply(
            topic: Topic,
            post: TopicPost?,
            content: String
        ): Observable<ReplyData.ReplyPost> {
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

            return ApiHelper.createHttpObservable(
                when (topic.model) {
                    "blog" -> "${Bangumi.SERVER}/blog/entry/${topic.id}"
                    else -> topic.url
                } + "/new_reply?ajax=1", body = data.build()
            ).map { rsp ->
                JsonUtil.toEntity<ReplyData>(rsp.body?.string() ?: "")?.posts
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
        ): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                topic.url + "/edit?ajax=1", body = FormBody.Builder()
                    .add("formhash", HttpUtil.formhash)
                    .add("title", title)
                    .add("submit", "改好了")
                    .add("content", content).build()
            ).map { rsp ->
                rsp.code == 200
            }
        }
    }
}