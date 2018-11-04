package soko.ekibun.bangumi.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.JsonUtil

class MainPresenter(private val context: MainActivity){
    private val api by lazy { Bangumi.createInstance() }
    private val auth by lazy { Bangumi.createInstance(false) }

    private val userModel by lazy{ UserModel(context) }
    private val themeModel by lazy{ ThemeModel(context) }
    private val userView = UserView(context, View.OnClickListener {
        val token = userModel.getToken()
        when {
            token == null -> {
                WebActivity.startActivityForAuth(context)}
            user == null -> refreshUser() //retry
            else -> WebActivity.launchUrl(context, user?.url)
        }
    })

    private val onLogout: ()->Unit = {
        userModel.saveToken(null)
        refreshUser()
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
        if(drawerView.checkedId != R.id.nav_home){
            drawerView.select(R.id.nav_home)
            return true
        }
        val homeTab = drawerView.homeFragment.frame_tab()?.getTabAt(0)?: return false
        if(homeTab.isSelected) return false
        homeTab.select()
        return true
    }

    private var userCall : Call<UserInfo>? = null
    fun refreshUser(){
        userCall?.cancel()

        val token = userModel.getToken()
        context.nav_view.menu.findItem(R.id.nav_logout).isVisible = token != null
        if(token != null){
            auth.refreshToken(token.refresh_token?:"").enqueue(ApiHelper.buildCallback(null,{
                if(it.expires_in==0){
                    Snackbar.make(context.window.decorView, "登录过期请重新登录", Snackbar.LENGTH_SHORT).show()
                    onLogout()
                }else{
                    userModel.saveToken(it)
                }
            }, {}))
            val user = userModel.getUser()
            userCall = api.user(token.user_id.toString())
            userCall?.enqueue(ApiHelper.buildCallback(context, {
                userModel.saveUser(it)
                setUser(it)
                if(user == null) refreshUser()
            },{}))
            if(user != null){
                setUser(user)
                drawerView.homeFragment.collectionFragment()?.reset()
            }
        }else{
            setUser(null)
            userModel.saveUser(null)
            drawerView.homeFragment.collectionFragment()?.reset()
        }
    }

    private var user: UserInfo? = null
    private fun setUser(user: UserInfo?){
        this.user = user
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
            setUser(JsonUtil.toEntity(userString, UserInfo::class.java))
    }

    private var tokenCall : Call<AccessToken>? = null
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                WebActivity.REQUEST_AUTH -> {
                    val code = data?.getStringExtra(WebActivity.RESULT_CODE)
                    if (code != null) {
                        tokenCall?.cancel()
                        tokenCall = auth.accessToken(code)
                        tokenCall?.enqueue(ApiHelper.buildCallback(context, {
                            userModel.saveToken(it)
                            refreshUser()
                        }, {}))
                    }
                }
            }
    }
}