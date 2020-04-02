package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import soko.ekibun.bangumi.App

/**
 * 插件类
 */
object PluginsModel {
    /**
     * 创建插件实例
     * @param context Context
     * @return Pair<Context, Any>?
     */
    fun createPluginInstance(context: Context): Map<Context, Any> {
        return context.packageManager.queryIntentServices(
            Intent("soko.ekibun.bangumi.plugins"), 0
        ).distinctBy { it.serviceInfo.packageName }.mapNotNull {
            try {
                val pluginContext = context.createPackageContext(
                    it.serviceInfo.packageName,
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                )
                val pluginClass = pluginContext.classLoader.loadClass(it.serviceInfo.name)
                pluginContext to pluginClass.getDeclaredConstructor().let {
                    it.isAccessible = true
                    it.newInstance()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.toMap()
    }

    /**
     * 应用插件
     * @param context Context
     * @return
     */
    fun setUpPlugins(activity: Activity): Int {
        val sp = PreferenceManager.getDefaultSharedPreferences(activity)
        if (!sp.getBoolean("plugins_enabled", true)) return 0
        return App.get(activity).pluginInstance.filter {
            if (!sp.getBoolean("use_plugin_${it.key.packageName}", true)) return@filter false
            try {
                val method =
                    it.value.javaClass.getMethod("setUpPlugins", Activity::class.java, Context::class.java)
                method.invoke(it.value, activity, it.key)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }.size
    }
}