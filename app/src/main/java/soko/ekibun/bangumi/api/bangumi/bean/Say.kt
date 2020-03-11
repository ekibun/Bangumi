package soko.ekibun.bangumi.api.bangumi.bean

import okhttp3.FormBody
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.getOrNull
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.collections.set

data class Say(
    val id: Int,
    var user: UserInfo,
    var message: String? = null,
    var time: String? = null,
    var replies: List<SayReply>? = null,
    var self: UserInfo? = null
) {
    val cacheKey = "say_$id"

    val url: String get() = "${Bangumi.SERVER}/user/${user.username}/timeline/status/$id"

    data class SayReply(
        val user: UserInfo,
        val message: String
    )

    companion object {
        /**
         * 从url解析吐槽
         * @param url String
         * @return Say?
         */
        fun parse(
            url: String,
            user: UserInfo? = null,
            message: String? = null,
            time: String? = null,
            self: UserInfo? = null
        ): Say? {
            val result = Regex("""/user/([^/]+)/timeline/status/(\d+)""").find(url)?.groupValues ?: return null
            return Say(
                id = result.getOrNull(2)?.toIntOrNull() ?: return null,
                user = user ?: UserInfo(
                    username = result.getOrNull(1) ?: return null
                ),
                message = message,
                time = time,
                self = self
            )
        }

        /**
         * 加载吐槽
         * @return Call<Say>
         */
        fun getSaySax(say: Say, onUpdate: (Say) -> Unit, onNewPost: (index: Int, post: SayReply) -> Unit): Call<Say> {
            return ApiHelper.buildHttpCall(say.url) { rsp ->
                val avatarCache = HashMap<String, String>()
                say.user.avatar?.let { avatarCache[say.user.username!!] = it }
                say.replies?.forEach { reply ->
                    reply.user.avatar?.let { avatarCache[reply.user.username!!] = it }
                }

                var beforeData = ""
                val replies = ArrayList<SayReply>()
                val updateReply = { str: String ->
                    val doc = Jsoup.parse(str)
                    doc.outputSettings().prettyPrint(false)
                    if (beforeData.isEmpty()) {
                        beforeData = str
                        val self = doc.selectFirst(".idBadgerNeue a.avatar")
                        say.user = UserInfo.parse(
                            doc.selectFirst(".statusHeader .inner a"),
                            Bangumi.parseImageUrl(doc.selectFirst(".statusHeader .avatar img"))
                        )
                        say.message = doc.selectFirst(".statusContent .text")?.html()
                        say.time = doc.selectFirst(".statusContent .date")?.text()
                        say.self = UserInfo.parse(self, Bangumi.parseImageUrl(self.selectFirst("span.avatarNeue")))
                        onUpdate(say)
                        onNewPost(0, SayReply(say.user, say.message ?: ""))
                    } else {
                        val user = UserInfo.parse(doc.selectFirst("a.l"))
                        user.avatar = avatarCache[user.username] ?: user.avatar ?: UserInfo.getApiUser(user).avatar
                        user.avatar?.let { avatarCache[user.username!!] = it }
                        val reply = SayReply(
                            user = user,
                            message = doc.selectFirst(".reply_item").childNodes()?.let { it.subList(6, it.size) }
                                ?.joinToString("") {
                                    it.outerHtml()
                                } ?: ""
                        )
                        replies += reply
                        onNewPost(replies.size, reply)
                    }
                }

                ApiHelper.parseWithSax(rsp) { parser, str ->
                    when {
                        parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                        parser.getAttributeValue("", "class")?.contains("reply_item") == true -> {
                            updateReply(str)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        parser.getAttributeValue("", "id") == "footer" -> {
                            updateReply(str)
                            ApiHelper.SaxEventType.END
                        }
                        else -> ApiHelper.SaxEventType.NOTHING
                    }
                }
                say.replies = replies
                say
            }
        }

        fun reply(say: Say, content: String): Call<Boolean> {
            return ApiHelper.buildHttpCall(
                "${Bangumi.SERVER}/timeline/${say.id}/new_reply?ajax=1",
                body = FormBody.Builder()
                    .add("content", content)
                    .add("formhash", HttpUtil.formhash)
                    .add("submit", "submit").build()
            ) {
                it.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }
    }
}