package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.module.LoadMoreModuleConfig
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.umeng.commonsdk.UMConfigure
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.util.HttpUtil

/**
 * App entry
 * @property dataCacheModel DataCacheModel
 * @property pluginInstance Map<Context, Any>?
 * @property remoteAction Function3<[@kotlin.ParameterName] Intent?, [@kotlin.ParameterName] Int, [@kotlin.ParameterName] Int, Unit>
 */
class App : Application() {
    val sp by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    val dataCacheModel by lazy { DataCacheModel(this) }
    lateinit var pluginInstance: Map<Context, Any>
    var remoteAction: (intent: Intent?, flags: Int, startId: Int) -> Unit = { _, _, _ -> }

    override fun onCreate() {
        super.onCreate()
        app = this

        HttpUtil.formhash = UserModel.userList.let { it.users[it.current] }?.formhash ?: HttpUtil.formhash
        ThemeModel.setTheme(this, ThemeModel.getTheme())
        UMConfigure.init(
            this,
            "5e68fe80167edd6e34000185",
            if (BuildConfig.DEBUG) "debug" else BuildConfig.FLAVOR,
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )

        LoadMoreModuleConfig.defLoadMoreView = object : BaseLoadMoreView() {

            override fun getRootView(parent: ViewGroup): View =
                parent.getItemView(R.layout.brvah_quick_view_load_more)

            override fun getLoadingView(holder: BaseViewHolder): View =
                holder.getView(R.id.load_more_loading_view)

            override fun getLoadComplete(holder: BaseViewHolder): View = View(holder.itemView.context)

            override fun getLoadEndView(holder: BaseViewHolder): View =
                holder.getView(R.id.load_more_load_end_view)

            override fun getLoadFailView(holder: BaseViewHolder): View =
                holder.getView(R.id.load_more_load_fail_view)
        }

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