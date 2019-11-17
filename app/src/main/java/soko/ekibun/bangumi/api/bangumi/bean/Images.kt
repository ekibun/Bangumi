package soko.ekibun.bangumi.api.bangumi.bean

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * bgm.tv图像类
 * @param url 图像url
 */
data class Images(private val url: String) {
    val medium: String = url.replace(Regex("/[lcmgs]/"), "/m/")
    val large: String = medium.replace("/m/", "/l/")
    val common: String = if (medium.contains(Regex("/(user|crt|icon)/"))) medium else medium.replace("/m/", "/c/")
    val small: String = medium.replace("/m/", "/s/")
    val grid: String = if (medium.contains(Regex("/(user|icon)/"))) medium else medium.replace("/m/", "/g/")

    /**
     * 返回设置的图像清晰度
     */
    fun getImage(context: Context): String {
        return when (PreferenceManager.getDefaultSharedPreferences(context).getString("image_quality", "c")) {
            "l" -> large
            "m" -> medium
            "s" -> small
            "g" -> grid
            else -> common
        }
    }
}