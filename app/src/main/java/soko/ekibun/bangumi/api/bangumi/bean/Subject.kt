package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 条目类
 */
data class Subject(
        val id: Int = 0,
        @SubjectType var type: Int = TYPE_ANY,
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
    val displayName = HttpUtil.html2text((if (name_cn.isNullOrEmpty()) name else name_cn) ?: "")

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
     * @property wish 想看
     * @property collect 已看
     * @property doing 在看
     * @property on_hold 搁置
     * @property dropped 抛弃
     */
    data class UserCollection(
            val wish: Int = 0,
            val collect: Int = 0,
            val doing: Int = 0,
            val on_hold: Int = 0,
            val dropped: Int = 0
    )

    data class Blog(
            var id: Int = 0,
            var title: String? = null,
            var summary: String? = null,
            var image: String? = null,
            var replies: Int = 0,
            var timestamp: Long = 0,
            var dateline: String? = null,
            var user: UserInfo? = null
    ){
        val url = "${Bangumi.SERVER}/blog/$id"
        /**
         * id : 273281
         * url : http://bgm.tv/blog/273281
         * title : 度没掌握好，方向也有偏离
         * summary : 度没掌握好，方向也有偏离。
         * 前面真的很棒，乡村微奇幻卖萌搞笑片，小町偶尔的卖蠢衬托出乡村少女的天真。
         * 可是随着片子的进展，（staff）欺负小町越来越过分，尤其是赤裸裸的表现出小町农村人的无知和自卑（被迫害妄想症），看着真是令人替她着急啊。
         * 如果说这些是为片子最后升华 ...
         * image :
         * replies : 7
         * timestamp : 1466485111
         * dateline : 2016-6-21 04:58
         * user : {"id":205577,"url":"http://bgm.tv/user/drawing","username":"drawing","nickname":"千叶铁矢","avatar":{"large":"http://lain.bgm.tv/pic/user/l/000/20/55/205577.jpg?r=1410168526","medium":"http://lain.bgm.tv/pic/user/m/000/20/55/205577.jpg?r=1410168526","small":"http://lain.bgm.tv/pic/user/s/000/20/55/205577.jpg?r=1410168526"},"formhash":null}
         */
    }

    data class Topic(
            var id: Int = 0,
            var title: String? = null,
            var main_id: Int = 0,
            var timestamp: Long = 0,
            var lastpost: Int = 0,
            var replies: Int = 0,
            var user: UserInfo? = null
    ) {
        val url = "${Bangumi.SERVER}/subject/topic/$id"
        /**
         * id : 1
         * url : http://bgm.tv/subject/topic/1
         * title : 拿这个来测试
         * main_id : 1
         * timestamp : 1216020847
         * lastpost : 1497657984
         * replies : 57
         * user : {"id":2,"url":"http://bgm.tv/user/2","username":"2","nickname":"陈永仁","avatar":{"large":"http://lain.bgm.tv/pic/user/l/000/00/00/2.jpg?r=1322480632","medium":"http://lain.bgm.tv/pic/user/m/000/00/00/2.jpg?r=1322480632","small":"http://lain.bgm.tv/pic/user/s/000/00/00/2.jpg?r=1322480632"},"formhash":null}
         */
    }

    @IntDef(TYPE_ANY, TYPE_BOOK, TYPE_ANIME, TYPE_MUSIC, TYPE_GAME, TYPE_REAL)
    annotation class SubjectType

    @StringDef(TYPE_NAME_BOOK, TYPE_NAME_ANIME, TYPE_NAME_MUSIC, TYPE_NAME_GAME, TYPE_NAME_REAL)
    annotation class SubjectTypeName

    companion object {
        /**
         * 条目类型定义
         */
        const val TYPE_ANY = 0
        const val TYPE_BOOK = 1
        const val TYPE_ANIME = 2
        const val TYPE_MUSIC = 3
        const val TYPE_GAME = 4
        const val TYPE_REAL = 6

        const val TYPE_NAME_BOOK = "book"
        const val TYPE_NAME_ANIME = "anime"
        const val TYPE_NAME_MUSIC = "music"
        const val TYPE_NAME_GAME = "game"
        const val TYPE_NAME_REAL = "real"

        /**
         * 条目类型Id->url请求字符串
         */
        @SubjectTypeName
        fun getTypeName(@SubjectType type: Int): String {
            return when (type) {
                TYPE_BOOK -> TYPE_NAME_BOOK
                TYPE_ANIME -> TYPE_NAME_ANIME
                TYPE_MUSIC -> TYPE_NAME_MUSIC
                TYPE_GAME -> TYPE_NAME_GAME
                TYPE_REAL -> TYPE_NAME_REAL
                else -> "subject"
            }
        }

        /**
         * 获取条目类型显示字符串
         */
        @StringRes
        fun getTypeRes(@SubjectType type: Int): Int {
            return when (type) {
                TYPE_BOOK -> R.string.book
                TYPE_ANIME -> R.string.anime
                TYPE_MUSIC -> R.string.music
                TYPE_GAME -> R.string.game
                TYPE_REAL -> R.string.real
                else -> R.string.subject
            }
        }
    }
}