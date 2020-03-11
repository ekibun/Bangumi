@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.setting

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import soko.ekibun.bangumi.BuildConfig
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.BaseFragmentActivity
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.AppUtil


/**
 * 设置Activity
 */
class SettingsActivity : BaseFragmentActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    var lastFragment: Fragment? = null

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        openPreferenceStartScreen(pref.key)
        title = pref.title
        return true
    }

    private fun openPreferenceStartScreen(key: String) {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key)

        fragment.arguments = args
        ft.replace(R.id.layout_content, fragment, key)
        ft.addToBackStack(key)
        ft.commit()
        lastFragment = fragment
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val fragment = supportFragmentManager.getFragment(savedInstanceState, "settings") ?: return
        val key = fragment.arguments?.getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT) ?: ""
        lastFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, fragment, key).commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastFragment?.let { supportFragmentManager.putFragment(outState, "settings", it) }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModel.setTheme(applicationContext, ThemeModel(applicationContext).getTheme())
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("pref_screen_title")?.let { title = it }
    }

    override fun onViewCreated(view: View) {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                setTitle(R.string.settings)
        }
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, SettingsFragment()).commit()
        intent?.getStringExtra("pref_screen_key")?.let { openPreferenceStartScreen(it) }
    }

    init {
        onBackListener = { supportFragmentManager.popBackStackImmediate() }
    }

    /**
     * 设置Fragment
     */
    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            val relativeLayout = RelativeLayout(layoutInflater.context)
            relativeLayout.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return relativeLayout
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            updatePreference()
            if (key == "pref_dark_mode") {
                activity?.let { ctx ->
                    val intent = Intent(ctx, ctx.javaClass)
                    intent.putExtra("pref_screen_key", preferenceScreen.key)
                    intent.putExtra("pref_screen_title", preferenceScreen.title)
                    ctx.finish()
                    startActivity(intent)
                    ctx.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey)
            updatePreference()
        }

        private fun updatePreference() {
            findPreference<ListPreference>("image_uploader")?.let { it.summary = it.entry }
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
            when (preference?.key) {
                "check_update_now" -> activity?.let {
                    AppUtil.checkUpdate(it, false) {
                        AlertDialog.Builder(it).setTitle("当前已是最新版")
                            .setPositiveButton(R.string.ok) { _, _ -> }.show()
                    }
                }
                "feed_back" -> activity?.let {
                    WebActivity.launchUrl(it, "https://bgm.tv/user/ekibun/timeline/status/20692126", "")
                }
            }

            return super.onPreferenceTreeClick(preference)
        }
    }
}
