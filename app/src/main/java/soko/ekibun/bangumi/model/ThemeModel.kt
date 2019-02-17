package soko.ekibun.bangumi.model

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate

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

        fun setTheme(context: Activity, night: Boolean){
            if((night && (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES)) || (!night && (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO))){
                AppCompatDelegate.setDefaultNightMode(if(night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                context.recreate()
            }
            //(context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).nightMode = if(night) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
        }
    }
}