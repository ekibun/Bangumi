package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject

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

    fun getVideoCacheList(context: Context): ArrayList<VideoCache>{
        val uri = Uri.parse("content://soko.ekibun.bangumiplayer/cache")
        val cursor = context.contentResolver.query(uri,
                arrayOf("_id", "data"), null, null, null)
        val ret = ArrayList<VideoCache>()
        while(cursor?.moveToNext() == true){
            ret += JsonUtil.toEntity(cursor.getString(1), VideoCache::class.java)?:continue
        }
        cursor?.close()
        return ret
    }
}