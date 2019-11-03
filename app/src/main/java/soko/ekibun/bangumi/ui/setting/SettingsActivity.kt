@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.setting

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.SwipeBackActivity


class SettingsActivity : SwipeBackActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = MyPreferenceFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        title = pref.title
        fragment.arguments = args
        ft.replace(R.id.settings_container, fragment, pref.key)
        ft.addToBackStack(pref.key)
        ft.commit()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                setTitle(R.string.settings)
        }

        supportFragmentManager.beginTransaction().replace(R.id.settings_container, MyPreferenceFragment()).commit()
    }

    override fun processBack() {
        if (!supportFragmentManager.popBackStackImmediate())
            super.processBack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
        }
        return super.onOptionsItemSelected(item)
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey)
        }
    }
}
