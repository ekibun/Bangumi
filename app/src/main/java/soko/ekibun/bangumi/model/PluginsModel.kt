package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
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
    fun createPluginInstance(context: Context): Pair<Context, Any>? {
        return try {
            val pluginContext = context.createPackageContext(
                "soko.ekibun.bangumi.plugin",
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            val pluginClass = pluginContext.classLoader.loadClass("soko.ekibun.bangumi.plugins.Plugin")
            pluginContext to pluginClass.getDeclaredConstructor().let {
                it.isAccessible = true
                it.newInstance()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 应用插件
     * @param context Context
     * @return Pair<Context, Any>?
     */
    fun setUpPlugins(activity: Activity): Boolean {
        val pluginInstance = App.get(activity).pluginInstance ?: return false
        return try {
            val method =
                pluginInstance.second.javaClass.getMethod("setUpPlugins", Activity::class.java, Context::class.java)
            method.invoke(pluginInstance.second, activity, pluginInstance.first)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}