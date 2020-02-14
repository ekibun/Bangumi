package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import android.content.Intent
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler

/**
 * App entry
 */
class App : Application() {
    val dataCacheModel by lazy { DataCacheModel(this) }
    var pluginInstance: Pair<Context, Any>? = null
    var remoteAction: (intent: Intent?, flags: Int, startId: Int) -> Unit = { _, _, _ -> }

    override fun onCreate() {
        super.onCreate()
        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        pluginInstance = PluginsModel.createPluginInstance(this)
        appContext = this
    }

    companion object {
        var appContext: App? = null

        fun get(context: Context): App {
            return context.applicationContext as App
        }
    }
}