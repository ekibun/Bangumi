package soko.ekibun.bangumi.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 主页Presenter
 */
class MainPresenter(private val context: MainActivity) {
    private val themeModel by lazy { ThemeModel(context) }
    private val userView = UserView(context, View.OnClickListener {
        when (user) {
            null -> {
                WebActivity.startActivityForAuth(context)
            }
            else -> WebActivity.launchUrl(context, user?.url)
        }
    })

    private val onLogout: () -> Unit = {
        if (HttpUtil.formhash.isNotEmpty()) {
            Bangumi.logout().enqueue(ApiHelper.buildCallback({
                refreshUser()
            }, {}))
        }
    }

    private val drawerView = DrawerView(context, { isChecked ->
        themeModel.saveTheme(isChecked)
        ThemeModel.setTheme(context, isChecked)
    }, onLogout)

    init {
        drawerView.switch.isChecked = themeModel.getTheme()
    }

    /**
     * 返回处理
     */
    fun processBack(): Boolean {
        if (context.drawer_layout.isDrawerOpen(GravityCompat.START)) {
            context.drawer_layout.closeDrawers()
            return true
        }
        if (drawerView.current()?.processBack() == true) {
            return true
        }
        if (drawerView.checkedId != R.id.nav_home) {
            drawerView.select(R.id.nav_home)
            return true
        }
        return false
    }

    var user: UserInfo? = null
    private fun updateUser(user: UserInfo?) {
        context.runOnUiThread {
            Log.v("updateUser", "${this.user}->$user")
            val lastName = this.user?.username
            this.user = user
            if (user == null || lastName != user.username) drawerView.homeFragment.onUserChange()
            context.nav_view.menu.findItem(R.id.nav_logout).isVisible = user != null
            userView.setUser(user)
        }
    }

    /**
     * 保存状态
     */
    fun onSaveInstanceState(outState: Bundle) {
        drawerView.onSaveInstanceState(outState)
        user?.let { outState.putString("user", JsonUtil.toJson(it)) }
    }

    /**
     * 恢复状态
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val userString = savedInstanceState.getString("user", "")
        drawerView.onRestoreInstanceState(savedInstanceState)
        if (!userString.isNullOrEmpty())
            updateUser(JsonUtil.toEntity<UserInfo>(userString) ?: return)
    }

    /**
     * 处理回调
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                WebActivity.REQUEST_AUTH -> {
                    refreshUser()
                }
            }
    }

    /**
     * 更新用户信息
     */
    fun refreshUser() {
        drawerView.homeFragment.updateUserCollection() ?: updateUserCollection()
    }

    var collectionList: List<Subject> = ArrayList()
    private var collectionCall: Call<List<Subject>>? = null
    var notify: Pair<Int, Int>? = null
    /**
     * 获取收藏
     */
    fun updateUserCollection(callback: (List<Subject>) -> Unit = {}, onError: (Throwable?) -> Unit = {}) {
        collectionCall?.cancel()
        collectionCall = Bangumi.getCollectionSax({ user ->
            updateUser(user)
        }, {
            notify = it
            context.runOnUiThread { context.notifyMenu?.badge = notify?.let { it.first + it.second } ?: 0 }
        })
        collectionCall?.enqueue(ApiHelper.buildCallback({
            collectionList = it
            callback(it)
        }, {
            onError(it)
            if ((it as? Exception)?.message == "login failed") (context as? MainActivity)?.mainPresenter?.updateUser(null)
        }))
    }
}