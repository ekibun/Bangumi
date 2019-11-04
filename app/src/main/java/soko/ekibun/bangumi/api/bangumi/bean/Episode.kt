package soko.ekibun.bangumi.api.bangumi.bean

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import java.text.DecimalFormat

/**
 * 剧集类
 * @property id 剧集id
 * @property type 剧集类型
 * @property sort 编号
 * @property name 标题
 * @property name_cn 中文标题
 * @property duration 时长
 * @property airdate 放送时间
 * @property comment 吐槽数
 * @property desc 简介
 * @property status 放送状态
 * @property progress 观看进度
 * @property category 音乐的专辑编号
 * @constructor
 */
data class Episode(
        val id: Int = 0,
        @EpisodeType var type: Int = 0,
        var sort: Float = 0f,
        var name: String? = null,
        var name_cn: String? = null,
        var duration: String? = null,
        var airdate: String? = null,
        var comment: Int = 0,
        var desc: String? = null,
        @EpisodeStatus var status: String? = null,
        @ProgressType var progress: String? = null,
        var category: String? = null
) {
    val url = "${Bangumi.SERVER}/ep/$id"

    fun merge(ep: Episode) {
        sort = if (sort == 0f) ep.sort else sort
        name = name ?: ep.name
        name_cn = name_cn ?: ep.name_cn
        duration = duration ?: ep.duration
        airdate = ep.airdate ?: ep.airdate
        comment = if (comment == 0) ep.comment else comment
        desc = desc ?: ep.desc
        status = status ?: ep.status
        progress = progress ?: ep.progress
        category = category ?: ep.category
    }


    fun parseSort(context: Context): String{
        return if(type == TYPE_MAIN)
            context.getString(R.string.parse_sort_ep, DecimalFormat("#.##").format(sort))
        else
            context.getString(getTypeRes(type)) + " ${DecimalFormat("#.##").format(sort)}"
    }

    @IntDef(TYPE_MAIN, TYPE_SP, TYPE_OP, TYPE_ED, TYPE_PV, TYPE_MAD, TYPE_OTHER, TYPE_MUSIC)
    annotation class EpisodeType

    @StringDef(STATUS_TODAY, STATUS_AIR, STATUS_NA)
    annotation class EpisodeStatus

    @StringDef(PROGRESS_WATCH, PROGRESS_QUEUE, PROGRESS_DROP, PROGRESS_REMOVE)
    annotation class ProgressType

    companion object {
        const val TYPE_MAIN = 0
        const val TYPE_SP = 1
        const val TYPE_OP = 2
        const val TYPE_ED = 3
        const val TYPE_PV = 4
        const val TYPE_MAD = 5
        const val TYPE_OTHER = 6
        const val TYPE_MUSIC = 7

        /**
         * 剧集类型字符串资源
         */
        @StringRes
        fun getTypeRes(@EpisodeType type: Int): Int {
            return when(type){
                TYPE_MAIN -> R.string.episode_type_main
                TYPE_SP -> R.string.episode_type_sp
                TYPE_OP -> R.string.episode_type_op
                TYPE_ED -> R.string.episode_type_ed
                TYPE_PV -> R.string.episode_type_pv
                TYPE_MAD -> R.string.episode_type_mad
                TYPE_MUSIC -> R.string.episode_type_music
                else -> R.string.episode_type_main
            }
        }

        const val STATUS_TODAY = "Today"
        const val STATUS_AIR = "Air"
        const val STATUS_NA = "NA"

        const val PROGRESS_WATCH = "watched"
        const val PROGRESS_QUEUE = "queue"
        const val PROGRESS_DROP = "drop"
        const val PROGRESS_REMOVE = "remove"

    }
}