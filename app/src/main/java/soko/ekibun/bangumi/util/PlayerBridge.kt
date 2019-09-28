package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.videoplayer.bean.VideoSubject

object PlayerBridge {
    const val EXTRA_SUBJECT = "extraSubject"
    const val EXTRA_EPISODE_LIST = "extraEpisodeList"

    fun checkActivity(context: Context, subject: Subject = Subject()): Boolean {
        val intent = parseIntent(subject)
        return context.packageManager.queryIntentActivities(intent, 0).size != 0
    }

    fun startActivity(context: Context, subject: Subject) {
        context.startActivity(parseIntent(subject))
    }

    private fun parseIntent(subject: Subject): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(when (subject.type) {
            Subject.TYPE_MUSIC -> "ekibun://player/music/${subject.id}"
            Subject.TYPE_BOOK -> "ekibun://player/book/${subject.id}"
            else -> "ekibun://player/bangumi/${subject.id}"
        }))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        intent.putExtra(EXTRA_SUBJECT, VideoSubject(subject))
        return intent
    }
}