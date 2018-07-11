package soko.ekibun.bangumi.api.bangumi.bean

data class Comment(
        val user: UserInfo? = null,
        val time: String? = null,
        val comment: String? = null,
        val rate: Int = 0
)