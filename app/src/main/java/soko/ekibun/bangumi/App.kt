package soko.ekibun.bangumi

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler
import soko.ekibun.bangumi.util.HttpUtil

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        HttpUtil.ua = WebView(this).settings.userAgentString

        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0)
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
}