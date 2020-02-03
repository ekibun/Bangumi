package soko.ekibun.bangumi.ui.view

import android.content.Intent
import android.os.Bundle
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

    override fun onStart() {
        super.onStart()
        ThemeModel.updateNavigationTheme(this)
    }

    var onActivityResultListener = { requestCode: Int, resultCode: Int, data: Intent? -> }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResultListener(requestCode, resultCode, data)
    }
}