package soko.ekibun.bangumi

import android.app.Application
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler

class App: Application(){
    override fun onCreate() {
        super.onCreate()
        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
}