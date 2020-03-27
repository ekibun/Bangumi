package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import android.content.Intent
import com.umeng.commonsdk.UMConfigure
import soko.ekibun.bangumi.model.*
import soko.ekibun.bangumi.util.HttpUtil

/**
 * App entry
 * @property dataCacheModel DataCacheModel
 * @property pluginInstance Map<Context, Any>?
 * @property remoteAction Function3<[@kotlin.ParameterName] Intent?, [@kotlin.ParameterName] Int, [@kotlin.ParameterName] Int, Unit>
 */
class App : Application() {
    val dataCacheModel by lazy { DataCacheModel(this) }
    val historyModel by lazy { HistoryModel(this) }
    val userModel by lazy { UserModel(this) }
    lateinit var pluginInstance: Map<Context, Any>
    var remoteAction: (intent: Intent?, flags: Int, startId: Int) -> Unit = { _, _, _ -> }

    override fun onCreate() {
        super.onCreate()
        HttpUtil.formhash = userModel.userList.let { it.users[it.current] }?.formhash ?: HttpUtil.formhash
        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        UMConfigure.init(
            this,
            "5e68fe80167edd6e34000185",
            if (BuildConfig.DEBUG) "debug" else BuildConfig.FLAVOR,
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )

        pluginInstance = PluginsModel.createPluginInstance(this)
        appContext = this
    }

    companion object {
        var appContext: App? = null

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