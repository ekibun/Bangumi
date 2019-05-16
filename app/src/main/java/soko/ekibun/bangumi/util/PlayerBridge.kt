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
    const val EXTRA_SUBJECT = "extraSubject"
    const val EXTRA_EPISODE_LIST = "extraEpisodeList"

    fun checkActivity(context: Context): Boolean {
        val intent =  parseIntent(VideoSubject())
        return context.packageManager.queryIntentActivities(intent, 0).size != 0
    }

    fun startActivity(context: Context, subject: VideoSubject) {
        context.startActivity(parseIntent(subject))
    }

    private fun parseIntent(subject: VideoSubject): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("ekibun://player/bangumi/${subject.id}"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        intent.putExtra(EXTRA_SUBJECT, subject)
        return intent
    }
}