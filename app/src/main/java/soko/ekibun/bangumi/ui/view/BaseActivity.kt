package soko.ekibun.bangumi.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel

/**
 * 基础Activity
 */
abstract class BaseActivity(@LayoutRes private val resId: Int) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(resId)

        ThemeModel.updateNavigationTheme(this)
        PluginsModel.setUpPlugins(this)
    }

    var onStartListener = { ThemeModel.updateNavigationTheme(this) }
    override fun onStart() {
        super.onStart()
        onStartListener()
    }

    var onStopListener = {}
    override fun onStop() {
        super.onStop()
        onStopListener()
    }

    var onDestroyListener = {}
    override fun onDestroy() {
        super.onDestroy()
        onDestroyListener()
    }

    var onActivityResultListener = { requestCode: Int, resultCode: Int, data: Intent? -> }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResultListener(requestCode, resultCode, data)
    }

    var onUserLeaveHintListener = {}
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onUserLeaveHintListener()
    }

    var onBackListener = { false }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (!onBackListener()) finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!onBackListener()) finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}