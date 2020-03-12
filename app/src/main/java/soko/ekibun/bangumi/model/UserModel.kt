package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.preference.PreferenceManager
import com.umeng.analytics.MobclickAgent
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

class UserModel(context: Context) {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    val userList by lazy {
        val list = JsonUtil.toEntity<UserStore>(
            sp.getString(PREF_USER, null) ?: JsonUtil.toJson(UserStore())
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
            } ?: {
                HttpUtil.formhash = ""
            }()
            sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
        }
        cookieManager.flush()
    }

    fun updateUser(user: UserInfo) {
        if (!userList.users.containsKey(user.id) && user.id > 0) {
            MobclickAgent.onProfileSignIn(user.id.toString())
        }
        userList.users[user.id] = UserStore.User(
            user = user,
            cookie = mapOf(
                Bangumi.COOKIE_HOST to (CookieManager.getInstance().getCookie(Bangumi.COOKIE_HOST) ?: ""),
                XSB_COOKIE_HOST to (CookieManager.getInstance().getCookie(XSB_COOKIE_HOST) ?: "")
            ),
            formhash = HttpUtil.formhash
        )
        sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
    }

    fun current(): UserInfo? {
        val current = userList.users[userList.current]?.user
        return current
    }

    fun removeUser(user: UserInfo): Boolean {
        val removed = userList.users.remove(user.id)
        if (removed != null) {
            MobclickAgent.onProfileSignOff()
        }
        if (userList.current == user.id) {
            switchToUser(userList.users.values.firstOrNull()?.user)
        }
        sp.edit().putString(PREF_USER, JsonUtil.toJson(userList)).apply()
        return removed != null
    }

    companion object {
        const val PREF_USER = "user"
        val XSB_COOKIE_HOST = "tinygrail.com"
    }
}