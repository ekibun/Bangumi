package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler

class App: Application(){
    val ua by lazy { WebView(this).settings.userAgentString }
    override fun onCreate() {
        super.onCreate()
        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        if(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0)
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }

    companion object {
        fun getUserAgent(context: Context): String {
            return (context.applicationContext as App).ua
        }
    }
}