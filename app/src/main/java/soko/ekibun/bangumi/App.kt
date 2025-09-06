package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.util.HttpUtil

/**
 * App entry
 * @property dataCacheModel DataCacheModel
 * @property pluginInstance Map<Context, Any>?
 */
class App : Application() {
    val sp by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    val dataCacheModel by lazy { DataCacheModel(this) }
    lateinit var pluginInstance: Map<Context, Any>

    override fun onCreate() {
        super.onCreate()
        app = this

        HttpUtil.formhash = UserModel.userList.let { it.users[it.current] }?.formhash ?: HttpUtil.formhash
        ThemeModel.setTheme(this, ThemeModel.getTheme())
        pluginInstance = PluginsModel.createPluginInstance(this)
    }

    companion object {
        lateinit var app: App

        /**
         * get from context
         * @param context Context
         * @return App
         */
        fun get(context: Context): App {
            return context.applicationContext as App
        }
    }
}