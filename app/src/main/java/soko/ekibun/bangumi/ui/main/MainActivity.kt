package soko.ekibun.bangumi.ui.main

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.search.SearchActivity
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.Toast
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.ui.view.NotifyActionProvider
import soko.ekibun.bangumi.ui.web.WebActivity

class MainActivity : AppCompatActivity() {

    private val mainPresenter by lazy{ MainPresenter(this) }
    val user get() = mainPresenter.user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        if(savedInstanceState?.containsKey("user") != true)
            mainPresenter.refreshUser()

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if(sp.getBoolean("check_update", true)){
            Github.createInstance().releases().enqueue(ApiHelper.buildCallback(this, {
                val release = it.firstOrNull()?:return@buildCallback
                if(release.tag_name?.compareTo(packageManager?.getPackageInfo(packageName, 0)?.versionName?:"")?:0 > 0 && sp.getString("ignore_tag", "") != release.tag_name)
                    AlertDialog.Builder(this)
                            .setTitle("新版本：${release.tag_name}")
                            .setMessage(release.body)
                            .setPositiveButton("下载"){_, _ ->
                                WebActivity.launchUrl(this@MainActivity, release.assets?.firstOrNull()?.browser_download_url, "")
                            }.setNegativeButton("忽略"){_, _ ->
                                sp.edit().putString("ignore_tag", release.tag_name).apply()
                            }.show()
            }) {})
        }
        mainPresenter.refreshUser{
            mainPresenter.reload()
            Bangumi.getNotify(ua).enqueue(ApiHelper.buildCallback(this, {
                notifyMenu?.badge = it.count()
            },{}))
        }
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    override fun onStart() {
        super.onStart()

        Bangumi.getNotify(ua).enqueue(ApiHelper.buildCallback(this, {
            notifyMenu?.badge = it.count()
        },{}))
    }

    var notifyMenu: NotifyActionProvider? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_main, menu)

        val menuItem = menu.findItem(R.id.action_notify)
        notifyMenu = MenuItemCompat.getActionProvider(menuItem) as NotifyActionProvider
        notifyMenu?.onClick = {
            WebActivity.launchUrl(this, "${Bangumi.SERVER}/notify")
        }

        return true
    }

    var exitTime = 0L
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if(mainPresenter.processBack()){
                return true
            }else if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(applicationContext, "再按一次退出程序", Toast.LENGTH_SHORT).show()
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
