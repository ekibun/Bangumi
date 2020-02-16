package soko.ekibun.bangumi.ui.main

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 主页Presenter
 * @property context MainActivity
 * @property userView UserView
 * @property onLogout Function0<Unit>
 * @property drawerView DrawerView
 * @property user UserInfo?
 * @property nav_view (com.google.android.material.navigation.NavigationView..com.google.android.material.navigation.NavigationView?)
 * @property nav_lp (android.view.ViewGroup.LayoutParams..android.view.ViewGroup.LayoutParams?)
 * @property calendar List<BangumiCalendarItem>
 * @property collectionList List<Subject>
 * @property collectionCall Call<List<Subject>>?
 * @property notify Pair<Int, Int>?
 * @constructor
 */
class MainPresenter(private val context: MainActivity) {
    private val userView = UserView(context, View.OnClickListener {
        when (user) {
            null -> {
                WebActivity.startActivityForAuth(context)
            }
            else -> WebActivity.startActivity(context, user?.url)
        }
    })

    private val onLogout: () -> Unit = {
        if (HttpUtil.formhash.isNotEmpty()) {
            Bangumi.logout().enqueue(ApiHelper.buildCallback({
                refreshUser()
            }, {}))
        }
    }

    val drawerView = DrawerView(context, onLogout)

    /**
     * 返回处理
     * @return Boolean
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

    val nav_view by lazy { context.nav_view }
    val nav_lp by lazy { nav_view.layoutParams }

    /**
     * 更新配置
     */
    fun updateConfiguration() {
        val lp = nav_lp
        (nav_view.parent as? ViewGroup)?.removeView(nav_view)
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                && !(Build.VERSION.SDK_INT > 24 && context.isInMultiWindowMode)
        if (isLandscape) {
            context.content_drawer.addView(nav_view, lp)
        } else {
            context.drawer_layout.addView(nav_view, lp)
        }
        nav_view.visibility = View.VISIBLE
        context.drawer_layout.closeDrawers()
        drawerView.toggle.isDrawerIndicatorEnabled = !isLandscape
    }

    /**
     * 保存状态
     * @param outState Bundle
     */
    fun onSaveInstanceState(outState: Bundle) {
        drawerView.onSaveInstanceState(outState)
        user?.let { outState.putString("user", JsonUtil.toJson(it)) }
    }

    /**
     * 恢复状态
     * @param savedInstanceState Bundle
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val userString = savedInstanceState.getString("user", "")
        drawerView.onRestoreInstanceState(savedInstanceState)
        if (!userString.isNullOrEmpty())
            updateUser(JsonUtil.toEntity<UserInfo>(userString) ?: return)
    }

    /**
     * 处理回调
     * @param requestCode Int
     * @param resultCode Int
     * @param data Intent?
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
        drawerView.homeFragment.updateUserCollection()
    }

    var calendar: List<BangumiCalendarItem> = App.get(context).dataCacheModel.get("calendar") ?: ArrayList()
    var collectionList: List<Subject> = ArrayList()
    private var collectionCall: Call<List<Subject>>? = null
    var notify: Pair<Int, Int>? = null
    /**
     * 获取收藏
     * @param callback Function1<List<Subject>, Unit>
     * @param onError Function1<Throwable?, Unit>
     */
    fun updateUserCollection(callback: (List<Subject>) -> Unit = {}, onError: (Throwable?) -> Unit = {}) {
        collectionCall?.cancel()
        collectionCall = Bangumi.getCollectionSax({ user ->
            updateUser(user)
        }, {
            notify = it
            context.runOnUiThread { context.notifyMenu?.badge = notify?.let { it.first + it.second } ?: 0 }
        }, {
            it.forEach { subject ->
                calendar.find { cal -> cal.id == subject.id }?.eps?.forEach { calEp ->
                    subject.eps?.find { ep -> ep.id == calEp.id }?.merge(calEp)
                }
            }
            collectionList = it
            context.runOnUiThread {
                drawerView.calendarFragment.onCollectionChange()
                callback(it)
            }
        })
        collectionCall?.enqueue(ApiHelper.buildCallback({}, {
            onError(it)
            if ((it as? Exception)?.message == "login failed")
                (context as? MainActivity)?.mainPresenter?.updateUser(null)
        }))
    }
}