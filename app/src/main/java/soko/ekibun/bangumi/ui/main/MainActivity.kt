package soko.ekibun.bangumi.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuItemCompat
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.BuildConfig
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.search.SearchActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.NotifyActionProvider
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.AppUtil

/**
 * 主页
 */
class MainActivity : BaseActivity(R.layout.activity_main) {
    val mainPresenter by lazy { MainPresenter(this) }
    val user get() = mainPresenter.user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (BuildConfig.AUTO_UPDATES && sp.getBoolean("check_update", true))
            AppUtil.checkUpdate(this)

        mainPresenter.updateConfiguration()
    }

    override fun onStart() {
        super.onStart()
        mainPresenter.refreshUser()
    }

    var notifyMenu: NotifyActionProvider? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_main, menu)

        val menuItem = menu.findItem(R.id.action_notify)
        notifyMenu = MenuItemCompat.getActionProvider(menuItem) as NotifyActionProvider
        notifyMenu?.onClick = {
            val inbox = mainPresenter.notify?.first ?: 0
            val notify = mainPresenter.notify?.second ?: 0
            val popup = PopupMenu(this, menuItem.actionView)
            popup.menuInflater.inflate(R.menu.list_notify, popup.menu)
            popup.menu.findItem(R.id.notify_type_inbox)?.title =
                "${getString(R.string.notify_inbox)}${if (inbox != 0) " (+$inbox)" else ""}"
            popup.menu.findItem(R.id.notify_type_notify)?.title =
                "${getString(R.string.notify_notify)}${if (notify != 0) " (+$notify)" else ""}"
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.notify_type_inbox -> {
                        mainPresenter.notify = Pair(0, mainPresenter.notify?.second ?: 0)
                        notifyMenu?.badge = mainPresenter.notify?.let { it.first + it.second } ?: 0
                        WebActivity.startActivity(this, "${Bangumi.SERVER}/pm")
                    }
                    R.id.notify_type_notify -> {
                        mainPresenter.notify = Pair(mainPresenter.notify?.first ?: 0, 0)
                        notifyMenu?.badge = mainPresenter.notify?.let { it.first + it.second } ?: 0
                        WebActivity.startActivity(this, "${Bangumi.SERVER}/notify" + if (notify == 0) "/all" else "")
                    }
                }
                true
            }
            popup.show()
        }

        return true
    }

    init {
        var exitTime = 0L
        onBackListener = {
            when {
                mainPresenter.processBack() -> {
                    true
                }
                (System.currentTimeMillis() - exitTime) > 2000 -> {
                    Toast.makeText(applicationContext, R.string.hint_back_to_close, Toast.LENGTH_SHORT).show()
                    exitTime = System.currentTimeMillis()
                    true
                }
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> SearchActivity.startActivity(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mainPresenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mainPresenter.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainPresenter.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mainPresenter.updateConfiguration()
    }

    companion object {
        /**
         * 启动
         */
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
