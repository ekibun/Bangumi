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

class ThemeModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    fun saveTheme(night: Boolean) {
        val editor = sp.edit()
        editor.putBoolean(PREF_NIGHT, night)
        editor.apply()
    }

    fun getTheme(): Boolean{
        return sp.getBoolean(PREF_NIGHT, false)
    }

    companion object {
        const val PREF_NIGHT="night"

        fun setTheme(context: Context, night: Boolean){
            val nightMode = if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
                if (context is Activity) {
                    context.recreate()
                }
            }
            //(context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).nightMode = if(night) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
        }

        fun updateNavigationTheme(activity: Activity) {
            updateNavigationTheme(activity.window, activity, false)
        }
        fun updateNavigationTheme(window: Window, context: Context, dialog: Boolean = true) {
            window.decorView.systemUiVisibility =  View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    (if(Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0) or
                    (if(dialog) View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN else 0)
            if(Build.VERSION.SDK_INT < 26) return
            val light = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
            if(light) window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            val color =  ResourceUtil.resolveColorAttr(context, android.R.attr.colorBackground)
            window.navigationBarColor = Color.argb( 200, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}