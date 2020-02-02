package soko.ekibun.bangumi.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.appbar_layout.*
import kotlinx.android.synthetic.main.base_activity.*
import soko.ekibun.bangumi.R

abstract class BaseFragmentActivity(@LayoutRes private val resId: Int? = null) : SwipeBackActivity() {
    val collapsibleAppBarHelper by lazy { CollapsibleAppBarHelper(app_bar as AppBarLayout) }
    abstract fun onViewCreated(view: View)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity)
        val view = if (resId != null) LayoutInflater.from(this).inflate(resId, layout_content) else layout_content
        onViewCreated(view)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = title

        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            view.dispatchApplyWindowInsets(insets.inset(0, insets.systemWindowInsetTop, 0, 0))
            insets
        }
    }

    override fun setTitle(title: CharSequence?) {
        collapsibleAppBarHelper.setTitle(title.toString(), supportActionBar?.subtitle?.toString())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}