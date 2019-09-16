package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.ArrayRes
import androidx.annotation.StringDef
import soko.ekibun.bangumi.R

/**
 * 条目收藏信息
 * @property status 收藏状态
 * @property rating 评分
 * @property comment 吐槽
 * @property private 自己可见
 * @property tag 标签
 * @constructor
 */
data class Collection(
        @CollectionStatusType val status: String? = null,
        val rating : Int = 0,
        val comment : String? = null,
        val private : Int = 0,
        val tag: List<String>? = null
) {
    val statusId = status?.let { arrayOf(TYPE_WISH, TYPE_COLLECT, TYPE_DO, TYPE_ON_HOLD, TYPE_DROPPED).indexOf(it) + 1 }
            ?: 0

    @StringDef(TYPE_WISH, TYPE_COLLECT, TYPE_DO, TYPE_ON_HOLD, TYPE_DROPPED)
    annotation class CollectionStatusType

    companion object {
        const val TYPE_WISH = "wish"
        const val TYPE_COLLECT = "collect"
        const val TYPE_DO = "do"
        const val TYPE_ON_HOLD = "on_hold"
        const val TYPE_DROPPED = "dropped"

        @CollectionStatusType
        fun getTypeById(id: Int): String {
            return arrayOf(TYPE_WISH, TYPE_COLLECT, TYPE_DO, TYPE_ON_HOLD, TYPE_DROPPED)[id - 1]
        }

        /**
         * 获取收藏类型字符串数组
         */
        @ArrayRes
        fun getTypeNamesRes(@Subject.SubjectType type: String): Int {
            return when (type) {
                Subject.TYPE_BOOK -> R.array.collection_status_book
                Subject.TYPE_MUSIC -> R.array.collection_status_music
                Subject.TYPE_GAME -> R.array.collection_status_game
                Subject.TYPE_REAL -> R.array.collection_status_real
                else -> R.array.collection_status_anime
            }
        }
    }
}