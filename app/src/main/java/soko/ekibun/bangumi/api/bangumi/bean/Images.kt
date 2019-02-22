package soko.ekibun.bangumi.api.bangumi.bean

import android.content.Context
import android.preference.PreferenceManager

data class Images(
        var large: String? = null,
        var common: String? = null,
        var medium: String? = null,
        var small: String? = null,
        var grid: String? = null
) {

    fun getImage(context: Context): String? {
        val quality = PreferenceManager.getDefaultSharedPreferences(context).getString("image_quality", "c")
        return when(quality){
            "l" -> large
            "m" -> medium
            "s" -> small
            "g" -> grid
            else -> common
        }
    }
    /**
     * large : http://lain.bgm.tv/pic/cover/l/e5/ba/6776_bA2h2.jpg
     * common : http://lain.bgm.tv/pic/cover/c/e5/ba/6776_bA2h2.jpg
     * medium : http://lain.bgm.tv/pic/cover/m/e5/ba/6776_bA2h2.jpg
     * small : http://lain.bgm.tv/pic/cover/s/e5/ba/6776_bA2h2.jpg
     * grid : http://lain.bgm.tv/pic/cover/g/e5/ba/6776_bA2h2.jpg
     */
}