package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 主题模块
 */
class ThemeModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    /**
     * 获取主题
     */
    fun getTheme(): Int {
        return sp.getString(PREF_NIGHT, "")?.toIntOrNull() ?: -1
    }

    companion object {
        const val PREF_NIGHT = "pref_dark_mode"

        /**
         * 应用主题
         */
        fun setTheme(context: Context, nightMode: Int) {
            if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
                if (context is Activity) {
                    context.recreate()
                }
            }
            //(context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).nightMode = if(night) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
        }

        /**
         * 应用导航栏主题
         */
        fun updateNavigationTheme(activity: Activity) {
            updateNavigationTheme(activity.window, activity, false)
        }

        /**
         * 应用导航栏主题
         */
        fun updateNavigationTheme(window: Window, context: Context, dialog: Boolean = true) {
            window.decorView.systemUiVisibility =  View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0)
            if(Build.VERSION.SDK_INT < 26) return
            val light = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
            if(light) window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            val color =  ResourceUtil.resolveColorAttr(context, android.R.attr.colorBackground)
            window.navigationBarColor = Color.argb( 200, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}