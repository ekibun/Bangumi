@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.setting

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import soko.ekibun.bangumi.BuildConfig
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.BaseFragmentActivity
import soko.ekibun.bangumi.util.AppUtil

/**
 * 设置Activity
 */
class SettingsActivity : BaseFragmentActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        title = pref.title
        fragment.arguments = args
        ft.replace(R.id.layout_content, fragment, pref.key)
        ft.addToBackStack(pref.key)
        ft.commit()
        return true
    }

    override fun onViewCreated(view: View) {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                setTitle(R.string.settings)
        }
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, SettingsFragment()).commit()
    }

    init {
        onBackListener = { supportFragmentManager.popBackStackImmediate() }
    }

    /**
     * 设置Fragment
     */
    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            updatePreference()
            if (key == "pref_dark_mode") {
                ThemeModel.setTheme(context ?: return, ThemeModel(context ?: return).getTheme())
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey)
            updatePreference()
        }

        private fun updatePreference() {
            findPreference<ListPreference>("pref_dark_mode")?.let { it.summary = it.entry }
            findPreference<ListPreference>("image_quality")?.let { it.summary = it.entry }
            findPreference<Preference>("check_update")?.isVisible = BuildConfig.AUTO_UPDATES
            findPreference<Preference>("check_update_now")?.summary =
                "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE} (${BuildConfig.FLAVOR})"
        }

        override fun onResume() {
            super.onResume()

            updatePreference()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if (preference?.key == "check_update_now") activity?.let {
                AppUtil.checkUpdate(it, false) {
                    AlertDialog.Builder(it).setTitle("当前已是最新版")
                        .setPositiveButton(R.string.ok) { _, _ -> }.show()
                }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}
