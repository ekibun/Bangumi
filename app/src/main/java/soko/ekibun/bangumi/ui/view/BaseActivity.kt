package soko.ekibun.bangumi.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.bangumi.model.ThemeModel

/**
 * 基础Activity
 */
abstract class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModel.updateNavigationTheme(this)
    }

    override fun onStart() {
        super.onStart()
        ThemeModel.updateNavigationTheme(this)
    }
}