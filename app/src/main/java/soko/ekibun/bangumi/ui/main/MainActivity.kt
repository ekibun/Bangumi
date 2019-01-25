package soko.ekibun.bangumi.ui.main

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.search.SearchActivity
import android.support.v4.view.MenuItemCompat
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import org.jsoup.Jsoup
import soko.ekibun.bangumi.ui.view.NotifyActionProvider
import soko.ekibun.bangumi.ui.web.WebActivity

class MainActivity : AppCompatActivity() {

    private val mainPresenter by lazy{ MainPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ThemeModel.setTheme(this, ThemeModel(this).getTheme())
        if(savedInstanceState?.containsKey("user") != true)
            mainPresenter.refreshUser()
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    var formhash = ""
    override fun onStart() {
        super.onStart()

        var needReload = false
        val cookieManager = CookieManager.getInstance()
        ApiHelper.buildHttpCall(Bangumi.SERVER, mapOf("User-Agent" to ua)){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            if(doc.selectFirst(".guest") != null) return@buildHttpCall null
            it.headers("set-cookie").forEach {
                needReload = true
                cookieManager.setCookie(Bangumi.SERVER, it) }
            if(needReload) mainPresenter.reload()
            doc.selectFirst("input[name=formhash]")?.attr("value")
        }.enqueue(ApiHelper.buildCallback(this, {hash->
            if(hash.isNullOrEmpty()) return@buildCallback
            formhash = hash?:formhash
            Bangumi.getNotify().enqueue(ApiHelper.buildCallback(this, {
                notifyMenu?.badge = it.count()
            },{}))
        }))
        Bangumi.getNotify().enqueue(ApiHelper.buildCallback(this, {
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
