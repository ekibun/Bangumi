package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
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
 * @property sp SharedPreferences
 * @constructor
 */
class ThemeModel(context: Context) {
    val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    /**
     * 获取主题
     * @return Int
     */
    fun getTheme(): Int {
        return sp.getString(PREF_NIGHT, "")?.toIntOrNull() ?: -1
    }

    companion object {
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
         * 获取一个 View 的缓存视图
         *
         * @param view
         * @return
         */
        private fun getCacheBitmapFromView(view: View): Bitmap? {
            val drawingCacheEnabled = true
            view.isDrawingCacheEnabled = drawingCacheEnabled
            view.buildDrawingCache(drawingCacheEnabled)
            val drawingCache = view.drawingCache
            val bitmap: Bitmap?
            if (drawingCache != null) {
                bitmap = Bitmap.createBitmap(drawingCache)
                view.isDrawingCacheEnabled = false
            } else {
                bitmap = null
            }
            return bitmap
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
}