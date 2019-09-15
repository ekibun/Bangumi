package soko.ekibun.bangumi.api.bangumi.bean

/**
 * 吐槽
 * @property user 用户
 * @property time 时间
 * @property comment 内容
 * @property rate 评分
 * @constructor
 */
data class Comment(
        val user: UserInfo? = null,
        val time: String? = null,
        val comment: String? = null,
        val rate: Int = 0
)