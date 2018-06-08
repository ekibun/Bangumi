package soko.ekibun.bangumi.api.bangumi.bean

import android.support.annotation.IntDef
import java.text.DecimalFormat


data class Episode(
        var id: Int = 0,
        var url: String? = null,
        @EpisodeType var type: Int = 0,
        var sort: Float = 0f,
        var name: String? = null,
        var name_cn: String? = null,
        var duration: String? = null,
        var airdate: String? = null,
        var comment: Int = 0,
        var desc: String? = null,
        var status: String? = null,
        var progress: SubjectProgress.EpisodeProgress? = null
) {
    /**
     * id : 4835
     * url : http://bgm.tv/ep/4835
     * type : 0
     * sort : 1
     * name : 廃部！
     * name_cn : 废部！
     * duration : 24m
     * airdate : 2009-04-03
     * comment : 12
     * desc : 春、新入生がクラブを決めるころ。田井中律は幼馴染の秋山澪を連れて軽音部の見学へ行く。
     * しかし部員全員が卒業してしまった軽音部は、新たに４人の部員が集まらないと廃部になってしまうという。
     * 合唱部の見学にきた琴吹紬を仲間に加え、最後の一人を探していると…。
     * status : Air
     */

    fun parseSort(): String{
        return if(type == TYPE_MAIN)
            "第 ${DecimalFormat("#.##").format(sort)} 话"
        else
            getTypeName(type) + " ${DecimalFormat("#.##").format(sort)}"
    }

    companion object {
        const val TYPE_MAIN = 0
        const val TYPE_SP = 1
        const val TYPE_OP = 2
        const val TYPE_ED = 3
        const val TYPE_PV = 4
        const val TYPE_MAD = 5
        const val TYPE_OTHER = 6

        @IntDef(TYPE_MAIN, TYPE_SP, TYPE_OP, TYPE_ED, TYPE_PV, TYPE_MAD, TYPE_OTHER)
        annotation class EpisodeType

        fun getTypeName(@EpisodeType type: Int): String{
            return when(type){
                TYPE_MAIN -> "本篇"
                TYPE_SP -> "特别篇"
                TYPE_OP -> "OP"
                TYPE_ED -> "ED"
                TYPE_PV -> "PV"
                TYPE_MAD -> "MAD"
                else -> "其他"
            }
        }
    }
}