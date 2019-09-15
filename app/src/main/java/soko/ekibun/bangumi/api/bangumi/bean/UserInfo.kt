package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 用户信息类
 */
data class UserInfo(
        var id: Int = 0,
        var username: String? = null,
        var nickname: String? = null,
        var avatar: Images? = null,
        var sign: String? = null,
        var notify: Pair<Int, Int>? = null
) {
    val url = "${Bangumi.SERVER}/user/$username"
}