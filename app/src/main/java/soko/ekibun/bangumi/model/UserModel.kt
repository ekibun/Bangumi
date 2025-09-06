package soko.ekibun.bangumi.model

import android.util.Log
import android.webkit.CookieManager
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

object UserModel {

    val userList by lazy {
        val list = JsonUtil.toEntity<UserStore>(
            App.app.sp.getString(PREF_USER, null) ?: JsonUtil.toJson(UserStore())
        ) ?: UserStore()
        list
    }

    init {
        if (userList.current < 0) userList.users.values.firstOrNull()?.user?.let { switchToUser(it) }
    }


    data class UserStore(
        var current: Int = -1,
        val users: HashMap<Int, User> = HashMap()
    ) {
        data class User(
            val user: UserInfo,
            val cookie: Map<String, String>?,
            val formhash: String
        )
    }

    fun switchToUser(user: UserInfo?) {
        val cookieManager = CookieManager.getInstance()
        if (userList.current != user?.id) {
            userList.current = user?.id ?: -1
            arrayOf(Bangumi.COOKIE_HOST, XSB_COOKIE_HOST).forEach { host ->
                Log.i("COOKIE", cookieManager.getCookie(host) ?: "")
                (cookieManager.getCookie(host) ?: "").split(';').forEach {
                    cookieManager.setCookie(host, it.split('=')[0].trim() + "=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                }
            }
            userList.users[user?.id ?: -1]?.let {
                it.cookie?.forEach { kv ->
                    kv.value.split(';').forEach { cookie ->
                        cookieManager.setCookie(kv.key, cookie)
                    }
                }
                HttpUtil.formhash = it.formhash
            } ?: run {
                HttpUtil.formhash = ""
            }
            App.app.sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
        }
        cookieManager.flush()
    }

    fun updateUser(user: UserInfo) {
        userList.users[user.id] = UserStore.User(
            user = user,
            cookie = mapOf(
                Bangumi.COOKIE_HOST to (CookieManager.getInstance().getCookie(Bangumi.COOKIE_HOST) ?: ""),
                XSB_COOKIE_HOST to (CookieManager.getInstance().getCookie(XSB_COOKIE_HOST) ?: "")
            ),
            formhash = HttpUtil.formhash
        )
        App.app.sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
    }

    fun current(): UserInfo? {
        return userList.users[userList.current]?.user
    }

    fun removeUser(user: UserInfo): Boolean {
        val removed = userList.users.remove(user.id)
        if (userList.current == user.id) {
            switchToUser(userList.users.values.firstOrNull()?.user)
        }
        App.app.sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
        return removed != null
    }

    const val PREF_USER = "user"
    val XSB_COOKIE_HOST = "https://.tinygrail.com"
}