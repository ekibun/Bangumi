package soko.ekibun.bangumi.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.Menu
import android.view.View
import android.widget.CompoundButton
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiCallback
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.auth.AuthActivity
import soko.ekibun.bangumi.ui.auth.AuthActivity.Companion.REQUEST_AUTH
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
                AuthActivity.startActivityForResult(context)}
            user == null -> refreshUser() //retry
            else -> CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(user?.url))
        }
    })

    private val onLogout: ()->Unit = {
        userModel.saveToken(null)
        setUser(null)
        drawerView.resetCollection()
        refreshUser()
    }

    private val drawerView = DrawerView(context, CompoundButton.OnCheckedChangeListener { view, isChecked ->
        themeModel.saveTheme(isChecked)
        ThemeModel.setTheme(context, isChecked)
    }, onLogout)

    init{
        drawerView.switch.isChecked = themeModel.getTheme()
    }

    private var userCall : Call<UserInfo>? = null
    fun refreshUser(){
        userCall?.cancel()
        setUser(null)
        drawerView.resetCollection()

        val token = userModel.getToken()
        if(token != null){
            /*val progressDialog = userView.createLoginProgressDialog(DialogInterface.OnCancelListener {
                userCall?.cancel()
            })
            progressDialog.show()*/
            userCall = api.user(token.user_id.toString())
            userCall?.enqueue(ApiCallback.build(context, {
                setUser(it)
                drawerView.resetCollection()
                api.refreshToken(token.refresh_token?:"").enqueue(ApiCallback.build(null,{
                    //userModel.saveToken(it)
                }))
            },{ /*progressDialog.dismiss()*/ }))
        }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        drawerView.onPrepareOptionsMenu(menu)
    }

    private var user: UserInfo? = null
    private fun setUser(user: UserInfo?){
        this.user = user
        userView.setUser(user)
        drawerView.setUser(user)
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
        when(requestCode) {
            REQUEST_AUTH -> {
                val code = data?.getStringExtra(AuthActivity.RESULT_CODE)
                if (code != null){
                    tokenCall?.cancel()
                    tokenCall = auth.accessToken(code)

                    /*val progressDialog = userView.createLoginProgressDialog(DialogInterface.OnCancelListener {
                        tokenCall?.cancel()
                    })
                    progressDialog.show()*/
                    tokenCall?.enqueue(ApiCallback.build(context, {
                        userModel.saveToken(it)
                        refreshUser()
                    },{ /*progressDialog.dismiss()*/ }))
                } }
        }
    }
}