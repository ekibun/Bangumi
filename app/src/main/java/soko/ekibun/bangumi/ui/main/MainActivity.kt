package soko.ekibun.bangumi.ui.main

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.search.SearchActivity
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import android.support.v4.view.MenuItemCompat
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

    override fun onStart() {
        super.onStart()
        BackgroundWebView(this).loadUrl(Bangumi.SERVER)
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
