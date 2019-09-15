package soko.ekibun.bangumi.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuItemCompat
import kotlinx.android.synthetic.main.activity_main.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.model.DownloadCacheProvider
import soko.ekibun.bangumi.ui.search.SearchActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.NotifyActionProvider
import soko.ekibun.bangumi.ui.web.WebActivity

class MainActivity : BaseActivity() {
    val downloadCacheProvider by lazy{ DownloadCacheProvider(this){
        nav_view.menu.findItem(R.id.nav_download).isVisible = it
    } }
    private val mainPresenter by lazy{ MainPresenter(this) }
    val user get() = mainPresenter.user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nav_view.menu.findItem(R.id.nav_download).isVisible = false
        downloadCacheProvider.bindService()

        if(savedInstanceState?.containsKey("user") != true)
            mainPresenter.refreshUser()

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if(sp.getBoolean("check_update", true)){
            Github.createInstance().releases().enqueue(ApiHelper.buildCallback({
                val release = it.firstOrNull()?:return@buildCallback
                val current = packageManager?.getPackageInfo(packageName, 0)?.versionName?:""
                if(!isFinishing && release.tag_name?.compareTo(current)?:0 > 0 && sp.getString("ignore_tag", "") != release.tag_name)
                    AlertDialog.Builder(this)
                            .setTitle( getString(R.string.parse_new_version, release.tag_name))
                            .setMessage( it.filter { it.tag_name?.compareTo(current)?:0 > 0 }.map { "${it.tag_name}\n${it.body}" }.reduce { acc, s -> "$acc\n$s" } )
                            .setPositiveButton(R.string.download){_, _ ->
                                WebActivity.launchUrl(this@MainActivity, release.assets?.firstOrNull()?.browser_download_url, "")
                            }.setNegativeButton(R.string.ignore){_, _ ->
                                sp.edit().putString("ignore_tag", release.tag_name).apply()
                            }.show()
            }) {})
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        downloadCacheProvider.unbindService()
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
            val inbox = mainPresenter.user?.notify?.first?:0
            val notify = mainPresenter.user?.notify?.second?:0
            val popup = PopupMenu(this, menuItem.actionView)
            popup.menuInflater.inflate(R.menu.list_notify, popup.menu)
            popup.menu.findItem(R.id.notify_type_inbox)?.title = "${getString(R.string.notify_inbox)}${if(inbox != 0) " (+$inbox)" else ""}"
            popup.menu.findItem(R.id.notify_type_notify)?.title = "${getString(R.string.notify_notify)}${if(notify != 0) " (+$notify)" else ""}"
            popup.setOnMenuItemClickListener{
                when(it.itemId){
                    R.id.notify_type_inbox -> WebActivity.launchUrl(this, "${Bangumi.SERVER}/pm")
                    R.id.notify_type_notify -> WebActivity.launchUrl(this, "${Bangumi.SERVER}/notify" + if(notify == 0) "/all" else "")
                }
                true
            }
            popup.show()
        }

        return true
    }

    var exitTime = 0L
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if(mainPresenter.processBack()){
                return true
            }else if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(applicationContext, R.string.hint_back_to_close, Toast.LENGTH_SHORT).show()
                exitTime = System.currentTimeMillis()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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

    companion object{
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
