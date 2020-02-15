package soko.ekibun.bangumi.api.bangumi.bean

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * bgm.tv图像类
 */
object Images {
    fun medium(url: String?): String = (url ?: "").replace(Regex("/[lcmgs]/"), "/m/")
    fun large(url: String?): String = medium(url).replace("/m/", "/l/")
    fun common(url: String?): String = medium(url).let { medium ->
        if (medium.contains(Regex("/(user|crt|icon)/"))) medium
        else medium.replace("/m/", "/c/")
    }

    fun small(url: String?): String = medium(url).replace("/m/", "/s/")
    fun grid(url: String?): String = medium(url).let { medium ->
        if (medium.contains(Regex("/(user|icon)/"))) medium
        else medium.replace("/m/", "/g/")
    }

    /**
     * 返回设置的图像清晰度
     * @param url String?
     * @param context Context
     * @return String
     */
    fun getImage(url: String?, context: Context): String {
        return when (PreferenceManager.getDefaultSharedPreferences(context).getString("image_quality", "c")) {
            "l" -> large(url)
            "m" -> medium(url)
            "s" -> small(url)
            "g" -> grid(url)
            else -> common(url)
        }
    }
}