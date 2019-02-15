package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.webkit.CookieManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import java.lang.Exception

object PlayerBridge {
    private const val EXTRA_SUBJECT = "extraSubject"
    private const val EXTRA_COOKIE = "extraCookie"

    fun checkActivity(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bangumi://player/0"))
        return context.packageManager.queryIntentActivities(intent, 0).size != 0
    }

    fun startActivity(context: Context, subject: Subject) {
        context.startActivity(parseIntent(subject))
    }

    private fun parseIntent(subject: Subject): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bangumi://player/${subject.id}"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
        intent.putExtra(EXTRA_COOKIE, CookieManager.getInstance().getCookie(Bangumi.SERVER))
        return intent
    }

    data class VideoCache (
            val bangumi: Subject,
            val videoList: Map<Int, VideoCacheBean>
    ){
        data class VideoCacheBean (
                val video: Episode,
                val url: String,
                val header: Map<String, String>
        )
    }

    private const val PACKAGE_NAME = "soko.ekibun.bangumiplayer"
    fun getVideoCacheList(context: Context): ArrayList<VideoCache>{
        return try {
            val bridgeContext = context.createPackageContext(PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
            val sp = PreferenceManager.getDefaultSharedPreferences(bridgeContext)
            ArrayList((JsonUtil.toEntity<Map<Int, VideoCache>>(sp.getString("videoCache", JsonUtil.toJson(HashMap<Int, VideoCache>()))!!,
                    object : TypeToken<Map<Int, VideoCache>>() {}.type) ?: HashMap()).values)
        }catch (e: Exception){
            e.printStackTrace()
            ArrayList() }
    }
}