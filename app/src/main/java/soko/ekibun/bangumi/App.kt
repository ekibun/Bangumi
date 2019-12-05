package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler

/**
 * App entry
 */
class App : Application() {
    val dataCacheModel by lazy { DataCacheModel(this) }
    override fun onCreate() {
        super.onCreate()
        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0)
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }

    companion object {
        fun get(context: Context): App {
            return context.applicationContext as App
        }
    }
}