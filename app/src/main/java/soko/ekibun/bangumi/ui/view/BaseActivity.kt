package soko.ekibun.bangumi.ui.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.umeng.analytics.MobclickAgent
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel

/**
 * 基础Activity
 * @property resId Int
 * @property onStartListener Function0<Unit>
 * @property onStopListener Function0<Unit>
 * @property onDestroyListener Function0<Unit>
 * @property onPauseListener Function0<Unit>
 * @property onResumeListener Function0<Unit>
 * @property onActivityResultListener Function3<Int, Int, Intent?, Unit>
 * @property onUserLeaveHintListener Function0<Unit>
 * @property onBackListener Function0<Boolean>
 * @constructor
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
        delegate.localNightMode = ThemeModel.getTheme()
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
        Log.v("destroy", "${this.javaClass}")
        onDestroyListener()
    }


    var onPauseListener = {}
    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
        onPauseListener()
    }

    var onResumeListener = {}
    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        onResumeListener()
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        try {
            super.onRestoreInstanceState(savedInstanceState)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}