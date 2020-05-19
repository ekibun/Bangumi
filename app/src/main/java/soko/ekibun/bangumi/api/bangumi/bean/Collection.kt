package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.ArrayRes
import androidx.annotation.StringDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Response
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 条目收藏信息
 * @property status String?
 * @property rating Int
 * @property comment String?
 * @property private Int
 * @property tag List<String>?
 * @property myTag List<String>?
 * @property statusId Int
 * @constructor
 */
data class Collection(
    @CollectionStatus val status: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val private: Int = 0,
    val tag: List<String>? = null,
    var myTag: List<String>? = null
) {
    val statusId
        get() = status?.let {
            arrayOf(STATUS_WISH, STATUS_COLLECT, STATUS_DO, STATUS_ON_HOLD, STATUS_DROPPED).indexOf(it) + 1
        } ?: 0

    /**
     * 收藏状态
     */
    @StringDef(STATUS_WISH, STATUS_COLLECT, STATUS_DO, STATUS_ON_HOLD, STATUS_DROPPED)
    annotation class CollectionStatus

    companion object {
        const val STATUS_WISH = "wish"
        const val STATUS_COLLECT = "collect"
        const val STATUS_DO = "do"
        const val STATUS_ON_HOLD = "on_hold"
        const val STATUS_DROPPED = "dropped"

        val statusArray = arrayOf(STATUS_WISH, STATUS_COLLECT, STATUS_DO, STATUS_ON_HOLD, STATUS_DROPPED)

        /**
         * 由id获取状态类型
         * @param id Int
         * @return String
         */
        @CollectionStatus
        fun getStatusById(id: Int): String {
            return statusArray[id - 1]
        }

        /**
         * 获取收藏类型字符串数组
         * @param type String
         * @return Int
         */
        @ArrayRes
        fun getStatusNamesRes(@Subject.SubjectType type: String): Int {
            return when (type) {
                Subject.TYPE_BOOK -> R.array.collection_status_book
                Subject.TYPE_MUSIC -> R.array.collection_status_music
                Subject.TYPE_GAME -> R.array.collection_status_game
                Subject.TYPE_REAL -> R.array.collection_status_real
                else -> R.array.collection_status_anime
            }
        }

        /**
         * 更新收藏
         * @param subject Subject
         * @param newCollection Collection
         * @return Call<Collection>
         */
        suspend fun updateStatus(
            subject: Subject,
            newCollection: Collection
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.getCall("${Bangumi.SERVER}/subject/${subject.id}/interest/update?gh=${HttpUtil.formhash}",
                    body = FormBody.Builder()
                        .add("referer", "ajax")
                        .add("interest", newCollection.statusId.toString())
                        .add("rating", newCollection.rating.toString())
                        .add(
                            "tags",
                            if (newCollection.tag?.isNotEmpty() == true) newCollection.tag.reduce { acc, s -> "$acc $s" } else "")
                        .add("comment", newCollection.comment ?: "")
                        .add("privacy", newCollection.private.toString())
                        .add("update", "保存").build()).execute()
            }
        }

        /**
         * 删除收藏
         * @param subject Subject
         * @return Call<Boolean>
         */
        suspend fun remove(
            subject: Subject
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.getCall("${Bangumi.SERVER}/subject/${subject.id}/remove?gh=${HttpUtil.formhash}").execute()
            }
        }
    }
}