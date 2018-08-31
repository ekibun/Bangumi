package soko.ekibun.bangumi

import android.app.Application
import soko.ekibun.bangumi.util.CrashHandler

class App: Application(){
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
}