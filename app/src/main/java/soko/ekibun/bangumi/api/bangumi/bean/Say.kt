package soko.ekibun.bangumi.api.bangumi.bean

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Response
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import java.util.concurrent.ConcurrentLinkedQueue

data class Say(
    val id: Int,
    var user: UserInfo,
    var message: String? = null,
    var time: String? = null,
    var replies: List<SayReply>? = null,
    var likes: List<TopicPost.Like>? = null
) {
    val cacheKey = "say_$id"

    val url: String get() = "${Bangumi.SERVER}/user/${user.username}/timeline/status/$id"

    data class SayReply(
        val user: UserInfo,
        val message: String,
        val index: Int
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
            time: String? = null
        ): Say? {
            val result = Regex("""/user/([^/]+)/timeline/status/(\d+)""").find(url)?.groupValues ?: return null
            return Say(
                id = result.getOrNull(2)?.toIntOrNull() ?: return null,
                user = user ?: UserInfo(
                    id = result.getOrNull(1)?.toIntOrNull() ?: 0,
                    username = result.getOrNull(1) ?: return null
                ),
                message = message,
                time = time
            )
        }

        /**
         * 加载吐槽
         * @return Call<SayReply|List<SayReply>|Say>
         */
        suspend fun getSaySax(
            say: Say,
            onUpdateSay: suspend (SayReply) -> Unit,
            onUpdate: suspend (List<SayReply>) -> Unit
        ) {
            withContext(Dispatchers.Default) {
                val rsp = HttpUtil.fetch(say.url)

                var replyCount = -1
                val replies = ConcurrentLinkedQueue<SayReply>()
                ApiHelper.parseSaxAsync(rsp, { tag, attrs ->
                    when {
                        attrs.contains("reply_item") -> {
                            replyCount++
                            replyCount to ApiHelper.SaxEventType.BEGIN
                        }
                        attrs.contains("id=\"footer\"") -> {
                            replyCount++
                            replyCount to ApiHelper.SaxEventType.END
                        }
                        else -> null to ApiHelper.SaxEventType.NOTHING
                    }
                }) { tag, str ->
                    val doc = Jsoup.parseBodyFragment(str)
                    doc.outputSettings().prettyPrint(false)

                    when (tag) {
                        0 -> {
                            val user = UserInfo.parse(doc.selectFirst(".statusHeader .inner a"))
                            user.avatar = user.avatar ?: "https://api.bgm.tv/v0/users/${user.username}/avatar?type=large"
                            say.user = user
                            say.message = doc.selectFirst(".statusContent .text")?.html()
                            say.time = doc.selectFirst(".statusContent .date")?.text()
                            withContext(Dispatchers.Main) { onUpdateSay(SayReply(say.user, say.message ?: "", 0)) }
                        }
                        else -> {
                            val user = UserInfo.parse(doc.selectFirst("a.l"))
                            user.avatar = user.avatar ?: "https://api.bgm.tv/v0/users/${user.username}/avatar?type=large"
                            val sayReply = SayReply(
                                user = user,
                                message = doc.selectFirst(".reply_item").childNodes()?.let { it.subList(6, it.size) }
                                    ?.joinToString("") {
                                        it.outerHtml()
                                    }?.trim() ?: "",
                                index = tag as Int
                            )
                            replies += sayReply
                            withContext(Dispatchers.Main) { onUpdate(listOf(sayReply)) }
                        }
                    }
                }
                say.replies = replies.sortedBy { it.index }
            }
        }

        suspend fun reply(say: Say, content: String): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    "${Bangumi.SERVER}/timeline/${say.id}/new_reply?ajax=1",
                    HttpUtil.RequestOption(
                        body = FormBody.Builder()
                            .add("content", content)
                            .add("formhash", HttpUtil.formhash)
                            .add("submit", "submit").build()
                    )
                )
            }
        }
    }
}