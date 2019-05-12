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
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.service.DialogActivity
import java.lang.Exception

object PlayerBridge {
    private const val EXTRA_SUBJECT = "extraSubject"
    private const val EXTRA_COOKIE = "extraCookie"

    fun checkActivity(context: Context, ua: String? = null): Boolean {
        val intent =  parseIntent(Subject(), ua)
        return context.packageManager.queryIntentActivities(intent, 0).size != 0
    }

    fun startActivity(context: Context, subject: Subject, ua: String? = null) {
        context.startActivity(parseIntent(subject, ua))
    }

    private fun parseIntent(subject: Subject, ua: String? = null): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if(ua != null) "ekibun://player/bangumi/${subject.id}" else "bangumi://player/${subject.id}"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        if(ua != null){
            intent.putExtra(DialogActivity.EXTRA_SUBJECT, VideoSubject(subject, ua))
        }else {
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_COOKIE, CookieManager.getInstance().getCookie(Bangumi.SERVER))
        }
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