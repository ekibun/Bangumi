package soko.ekibun.bangumi.ui.view

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.PluginsModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.ResourceUtil


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
abstract class BaseActivity(@LayoutRes private val resId: Int) : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(ColorDrawable(ResourceUtil.resolveColorAttr(this, R.attr.colorPrimaryBackground)))
        setContentView(resId)

        ThemeModel.updateNavigationTheme(this)

        PluginsModel.setUpPlugins(this)
    }

    private val jobCollection = HashMap<String, Job>()
    fun cancel(check: (String) -> Boolean) {
        jobCollection.keys.forEach {
            if (check(it)) jobCollection.remove(it)
        }
    }

    fun subscribe(
        onError: (t: Throwable) -> Unit = {},
        key: String? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        if (!key.isNullOrEmpty()) jobCollection[key]?.cancel()
        return launch {
            try {
                block.invoke(this)
            } catch (_: CancellationException) {
            } catch (t: Throwable) {
                Toast.makeText(App.app, t.message, Toast.LENGTH_SHORT).show()
                t.printStackTrace()
                onError(t)
            }
        }.also {
            if (!key.isNullOrEmpty()) jobCollection[key] = it
        }
    }

    var onStartListener = {}
    override fun onStart() {
        super.onStart()
        delegate.localNightMode = ThemeModel.getTheme()
        ThemeModel.updateNavigationTheme(this)
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
        cancel()
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
            android.R.id.home -> {
                if (!onBackListener()) finish()
                return true
            }
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