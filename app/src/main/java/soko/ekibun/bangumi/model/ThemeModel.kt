package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.util.ResourceUtil


/**
 * 主题模块
 */
object ThemeModel {

    /**
     * 获取主题
     * @return Int
     */
    fun getTheme(): Int {
        return App.app.sp.getString(PREF_NIGHT, "")?.toIntOrNull() ?: -1
    }

    const val PREF_NIGHT = "pref_dark_mode"

    /**
     * 应用主题
     * @param context Context
     * @param nightMode Int
     */
    fun setTheme(context: Context, nightMode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
            if (context is Activity) context.recreate()
        }
    }

    /**
     * 应用导航栏主题
     * @param activity Activity
     */
    fun updateNavigationTheme(activity: Activity) {
        updateNavigationTheme(activity.window, activity)
    }

    /**
     * 应用导航栏主题
     * @param window Window
     * @param context Context
     */
    fun updateNavigationTheme(window: Window, context: Context) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0)
        if (Build.VERSION.SDK_INT < 26) return
        val light = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
        if (light) window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val color = ResourceUtil.resolveColorAttr(context, android.R.attr.colorBackground)
        window.navigationBarColor = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * 全屏
     * @param window Window
     */
    fun fullScreen(window: Window) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}