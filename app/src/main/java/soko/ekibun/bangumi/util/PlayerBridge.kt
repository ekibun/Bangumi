package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.Subject

object PlayerBridge {
    private const val PACKAGE_NAME = "soko.ekibun.bangumiplayer"
    private const val ACTIVITY_NAME = "$PACKAGE_NAME.ui.video.VideoActivity"

    private const val EXTRA_SUBJECT = "extraSubject"
    private const val EXTRA_TOKEN = "extraToken"

    fun checkActivity(context: Context): Boolean {
        val intent = Intent().setClassName(PACKAGE_NAME, ACTIVITY_NAME)
        return context.packageManager.queryIntentActivities(intent, 0).size != 0
    }

    fun startActivity(context: Context, subject: Subject, token: AccessToken?) {
        context.startActivity(parseIntent(subject, token))
    }

    private fun parseIntent(subject: Subject, token: AccessToken?): Intent {
        val intent = Intent().setClassName(PACKAGE_NAME, ACTIVITY_NAME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
        intent.putExtra(EXTRA_TOKEN, JsonUtil.toJson(token?:AccessToken()))
        return intent
    }
}