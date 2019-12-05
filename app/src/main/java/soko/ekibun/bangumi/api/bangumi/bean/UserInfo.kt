package soko.ekibun.bangumi.api.bangumi.bean

import android.webkit.CookieManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 用户信息类
 */
data class UserInfo(
        var id: Int = 0,
        var username: String? = null,
        var nickname: String? = null,
        var avatar: String? = null,
        var sign: String? = null
) {
    val url = "${Bangumi.SERVER}/user/$username"

    companion object {
        /**
         * 从url中提取用户名
         */
        fun getUserName(href: String?): String? {
            return Regex("""/user/([^/]*)""").find(href ?: "")?.groupValues?.get(1)
        }

        /**
         * 获取用户信息
         */
        fun parse(user: Element?, avatar: String? = null): UserInfo {
            val username = getUserName(user?.attr("href"))
            return UserInfo(
                    id = username?.toIntOrNull() ?: 0,
                    username = username,
                    nickname = user?.text(),
                    avatar = avatar
            )
        }

        /**
         * 自己的用户信息
         */
        fun getSelf(reload: () -> Unit): Call<UserInfo> {
            val cookieManager = CookieManager.getInstance()
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/settings") {
                val doc = Jsoup.parse(it.body?.string() ?: "")
                val user = doc.selectFirst(".idBadgerNeue a.avatar") ?: throw Exception("login failed")
                val username = getUserName(user.attr("href"))
                it.headers("set-cookie").forEach {
                    cookieManager.setCookie(Bangumi.SERVER, it)
                }
                if (it.headers("set-cookie").isNotEmpty()) reload()
                HttpUtil.formhash = doc.selectFirst("input[name=formhash]")?.attr("value") ?: HttpUtil.formhash
                UserInfo(
                        id = username?.toIntOrNull() ?: 0,
                        username = username,
                        nickname = doc.selectFirst("input[name=nickname]")?.attr("value"),
                        avatar = Bangumi.parseImageUrl(user.selectFirst("span.avatarNeue")),
                        sign = doc.selectFirst("input[name=sign_input]")?.attr("value")
                )
            }
        }
    }
}