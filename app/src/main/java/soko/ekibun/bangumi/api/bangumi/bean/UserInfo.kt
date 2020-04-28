package soko.ekibun.bangumi.api.bangumi.bean

import org.jsoup.nodes.Element
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.util.*

/**
 * 用户信息类
 * @property username String?
 * @property nickname String?
 * @property avatar String?
 * @property sign String?
 * @property url String
 * @constructor
 */
data class UserInfo(
    var id: Int = 0,
    var username: String? = null,
    var nickname: String? = null,
    var avatar: String? = null,
    var sign: String? = null
) {
    val url = "${Bangumi.SERVER}/user/$username"

    val name get() = if (nickname.isNullOrEmpty()) username else nickname

    companion object {
        /**
         * 从url中提取用户名
         * @param href String?
         * @return String?
         */
        fun getUserName(href: String?): String? {
            return Regex("""/user/([^/]*)""").find(href ?: "")?.groupValues?.get(1)
        }

        /**
         * 获取用户信息
         * @param user Element?
         * @param avatar String?
         * @return UserInfo
         */
        fun parse(user: Element?, avatar: String? = null): UserInfo {
            val username = getUserName(user?.attr("href"))
            val userId = username?.toIntOrNull()
            return UserInfo(
                id = username?.toIntOrNull() ?: 0,
                username = username,
                nickname = user?.text(),
                avatar = avatar ?: userId?.let {
                    String.format(
                        "https://lain.bgm.tv/pic/user/l/%03d/%02d/%02d/%d.jpg",
                        it / 1000000, it / 10000 % 100, it / 100 % 100, it
                    )
                }
            )
        }

        private val userCache = WeakHashMap<String, UserInfo>()
        fun getApiUser(username: String): UserInfo {
            return userCache.getOrPut(username) {
                JsonUtil.toJsonObject(
                    HttpUtil.getCall(
                        "https://api.bgm.tv/user/${username}"
                    ).execute().body?.string() ?: ""
                ).let { obj ->
                    UserInfo(
                        id = obj.get("id")?.asInt ?: 0,
                        username = obj.get("username")?.asString,
                        nickname = obj.get("nickname")?.asString,
                        avatar = obj.getAsJsonObject("avatar")?.get("large")?.asString,
                        sign = obj.get("sign")?.asString
                    )
                }
            }
        }
    }
}