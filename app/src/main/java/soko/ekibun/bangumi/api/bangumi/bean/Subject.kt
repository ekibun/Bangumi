package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.StringDef
import androidx.annotation.StringRes
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.TextUtil

/**
 * 条目类
 */
data class Subject(
        val id: Int = 0,
        @SubjectType var type: String = TYPE_ANY,
        var name: String? = null,
        var name_cn: String? = null,
        var summary: String? = null,
        var images: Images? = null,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var infobox: List<Pair<String, String>>? = null,
        var category: String? = null,

        var rating: UserRating? = null,
        var collection: UserCollection? = null,

        var eps: List<Episode>? = null,
        var eps_count: Int = 0,
        var vol_count: Int = 0,
        var ep_status: Int = 0,
        var vol_status: Int = 0,

        var crt: List<Character>? = null,
        var staff: List<Person>? = null,
        var topic: List<Topic>? = null,
        var blog: List<Blog>? = null,
        //web
        var linked: List<Subject>? = null,
        var recommend: List<Subject>? = null,
        var tags: List<Pair<String, Int>>? = null,
        var collect: Collection? = null
) {
    val url = "${Bangumi.SERVER}/subject/$id"

    /**
     * 显示用的条目名称
     */
    val displayName = TextUtil.html2text((if (name_cn.isNullOrEmpty()) name else name_cn) ?: "")

    /**
     * 条目评分
     * @property rank 排名
     * @property total 总评分人数
     * @property count 评分分布
     * @property score 平均评分
     * @property friend_score 朋友评分
     * @property friend_count 朋友评分人数
     */
    class UserRating(
            val rank: Int = 0,
            val total: Int = 0,
            val count: IntArray = IntArray(10),
            val score: Float = 0f,
            val friend_score: Float = 0f,
            val friend_count: Int = 0
    )

    /**
     * 条目收藏
     */
    data class UserCollection(
            val wish: Int = 0,
            val collect: Int = 0,
            val doing: Int = 0,
            val on_hold: Int = 0,
            val dropped: Int = 0
    )

    /**
     * 日志
     */
    data class Blog(
            val id: Int = 0,
            val title: String? = null,
            val summary: String? = null,
            val image: String? = null,
            val replies: Int = 0,
            val time: String? = null,
            val user: UserInfo? = null
    ){
        val url = "${Bangumi.SERVER}/blog/$id"
    }

    /**
     * 评论
     */
    data class Topic(
            val id: Int = 0,
            val title: String = "",
            val time: String? = null,
            val replies: Int = 0,
            val user: UserInfo? = null
    ) {
        val url = "${Bangumi.SERVER}/subject/topic/$id"
    }

    @StringDef(TYPE_ANY, TYPE_BOOK, TYPE_ANIME, TYPE_MUSIC, TYPE_GAME, TYPE_REAL)
    annotation class SubjectType

    companion object {
        /**
         * 条目类型定义
         */
        const val TYPE_ANY = "any"
        const val TYPE_BOOK = "book"
        const val TYPE_ANIME = "anime"
        const val TYPE_MUSIC = "music"
        const val TYPE_GAME = "game"
        const val TYPE_REAL = "real"

        /**
         * 获取条目类型显示字符串
         */
        @StringRes
        fun getTypeRes(@SubjectType type: String): Int {
            return when (type) {
                TYPE_BOOK -> R.string.book
                TYPE_ANIME -> R.string.anime
                TYPE_MUSIC -> R.string.music
                TYPE_GAME -> R.string.game
                TYPE_REAL -> R.string.real
                else -> R.string.subject
            }
        }

        @SubjectType
        fun parseType(type: Int?): String {
            return when (type) {
                1 -> TYPE_BOOK
                2 -> TYPE_ANIME
                3 -> TYPE_MUSIC
                4 -> TYPE_GAME
                6 -> TYPE_REAL
                else -> TYPE_ANY
            }
        }
    }
}