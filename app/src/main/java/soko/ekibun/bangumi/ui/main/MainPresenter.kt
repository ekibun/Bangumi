package soko.ekibun.bangumi.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import kotlinx.android.synthetic.main.activity_main.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.JsonUtil
import java.lang.Exception

class MainPresenter(private val context: MainActivity){
    private val themeModel by lazy{ ThemeModel(context) }
    private val userView = UserView(context, View.OnClickListener {
        when (user) {
            null -> { WebActivity.startActivityForAuth(context) }
            else -> WebActivity.launchUrl(context, user?.url)
        }
    })

    private val onLogout: ()->Unit = {
        val formhash = user?.sign?:""
        if(formhash.isNotEmpty()){
            ApiHelper.buildHttpCall("${Bangumi.SERVER}/logout/$formhash", mapOf("User-Agent" to context.ua)){
                val cookieManager = CookieManager.getInstance()
                it.headers("set-cookie").forEach { cookieManager.setCookie(Bangumi.SERVER, it) }
                it.code()
            }.enqueue(ApiHelper.buildCallback(context, {
                refreshUser()
            }, {}))
        }
    }

    private val drawerView = DrawerView(context, {isChecked ->
        themeModel.saveTheme(isChecked)
        ThemeModel.setTheme(context, isChecked)
    }, onLogout)

    init{
        drawerView.switch.isChecked = themeModel.getTheme()
    }

    fun processBack(): Boolean{
        if(context.drawer_layout.isDrawerOpen(GravityCompat.START)){
            context.drawer_layout.closeDrawers()
            return true
        }
        if(drawerView.current()?.processBack() == true){
            return true
        }
        if(drawerView.checkedId != R.id.nav_home){
            drawerView.select(R.id.nav_home)
            return true
        }
        return false
    }

    fun reload(){
        drawerView.homeFragment.timelineFragment()?.onSelect()
    }

    fun refreshUser(reload: ()->Unit = {}){
        Bangumi.getUserInfo(context.ua).enqueue(ApiHelper.buildCallback(null, {
            updateUser(it)
            if(it.needReload) reload()
        }, { if((it as? Exception)?.message == "login failed") updateUser(null) }))
    }

    var user: UserInfo? = null
    private fun updateUser(user: UserInfo?){
        Log.v("updateUser", "${this.user}->$user")
        if(this.user != user || user == null) {
            this.user = user
            drawerView.homeFragment.collectionFragment()?.reset()
        }
        context.nav_view.menu.findItem(R.id.nav_logout).isVisible = user != null
        userView.setUser(user)
    }

    fun onSaveInstanceState(outState: Bundle){
        drawerView.onSaveInstanceState(outState)
        user?.let{ outState.putString("user", JsonUtil.toJson(it)) }
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle){
        val userString = savedInstanceState.getString("user", "")
        drawerView.onRestoreInstanceState(savedInstanceState)
        if(!userString.isNullOrEmpty())
            updateUser(JsonUtil.toEntity(userString, UserInfo::class.java)?: return)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                WebActivity.REQUEST_AUTH -> {
                    refreshUser()
                }
            }
    }
}