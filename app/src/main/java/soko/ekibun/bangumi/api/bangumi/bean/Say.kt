package soko.ekibun.bangumi.api.bangumi.bean

import okhttp3.FormBody
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

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
        fun getSay(say: Say): Call<Say> {
            return ApiHelper.buildHttpCall(say.url) { rsp ->
                val avatarCache = HashMap<String, String>()
                say.user.avatar?.let { avatarCache[say.user.username!!] = it }
                say.replies?.forEach { reply ->
                    reply.user.avatar?.let { avatarCache[reply.user.username!!] = it }
                }

                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.outputSettings().prettyPrint(false)

                val self = doc.selectFirst(".idBadgerNeue a.avatar")
                Say(
                    id = say.id,
                    user = UserInfo.parse(
                        doc.selectFirst(".statusHeader .inner a"),
                        Bangumi.parseImageUrl(doc.selectFirst(".statusHeader .avatar img"))
                    ),
                    message = doc.selectFirst(".statusContent .text")?.html(),
                    time = doc.selectFirst(".statusContent .date")?.text(),
                    replies = doc.select(".reply_item").map { item ->
                        val user = UserInfo.parse(item.selectFirst("a.l"))
                        user.avatar = avatarCache[user.username] ?: user.avatar ?: UserInfo.getApiUser(user).avatar
                        user.avatar?.let { avatarCache[user.username!!] = it }
                        SayReply(
                            user = user,
                            message = item.childNodes()?.let { it.subList(6, it.size) }?.joinToString("") {
                                it.outerHtml()
                            } ?: ""
                        )
                    },
                    self = UserInfo.parse(self, Bangumi.parseImageUrl(self.selectFirst("span.avatarNeue")))
                )
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