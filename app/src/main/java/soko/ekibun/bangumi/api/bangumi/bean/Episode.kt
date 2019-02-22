package soko.ekibun.bangumi.api.bangumi.bean

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.IntDef
import soko.ekibun.bangumi.R
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
        var progress: SubjectProgress.EpisodeProgress? = null,
        //music
        var cat: String? = null
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

    fun parseSort(context: Context): String{
        return if(type == TYPE_MAIN)
            context.getString(R.string.parse_sort_ep, DecimalFormat("#.##").format(sort))
        else
            context.getString(getTypeName(type)) + " ${DecimalFormat("#.##").format(sort)}"
    }

    companion object {
        const val TYPE_MAIN = 0
        const val TYPE_SP = 1
        const val TYPE_OP = 2
        const val TYPE_ED = 3
        const val TYPE_PV = 4
        const val TYPE_MAD = 5
        const val TYPE_OTHER = 6
        const val TYPE_MUSIC = 7

        @IntDef(TYPE_MAIN, TYPE_SP, TYPE_OP, TYPE_ED, TYPE_PV, TYPE_MAD, TYPE_OTHER, TYPE_MUSIC)
        annotation class EpisodeType

        @SuppressLint("SwitchIntDef")
        fun getTypeName(@EpisodeType type: Int): Int{
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
    }
}