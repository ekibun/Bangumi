package soko.ekibun.bangumi.ui.main

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import retrofit2.Call
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 主页Presenter
 * @property context MainActivity
 * @property userView UserView
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
                switchUser = null
                WebActivity.startActivityForAuth(context)
            }
            else -> WebActivity.startActivity(context, user?.url)
        }
    })

    private val userModel = App.get(context).userModel

    private val user get() = userModel.current()

    val drawerView = DrawerView(context) {
        user?.let {
            userModel.removeUser(it)
            updateUser(user)
            refreshUser()
        }
    }

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

    var lastUser: UserInfo? = UserInfo(username = "/")
    private fun updateUser(user: UserInfo?) {
        context.runOnUiThread {
            Log.v("updateUser", "${lastUser?.username}->${user?.username}")
            if (lastUser?.username != user?.username) {
                collectionList = ArrayList()
                drawerView.homeFragment.onUserChange()
            }
            lastUser = user
            context.nav_view.menu.findItem(R.id.nav_logout).isVisible = user != null
            userView.setUser(user)
        }
    }

    var switchUser: UserInfo? = null

    init {
        userView.headerView.user_figure.setOnLongClickListener {
            val popup = PopupMenu(context, userView.headerView.user_figure)
            userModel.userList.users.values.forEach {
                popup.menu.add(0, it.user.id, 0, "${it.user.nickname}@${it.user.username}")
            }
            popup.menu.add("添加账号")
            popup.setOnMenuItemClickListener {
                switchUser = userModel.current()
                val user = userModel.userList.users[it.itemId]?.user
                userModel.switchToUser(user)
                if (lastUser?.username != user?.username) collectionCall?.cancel()
                if (user != null) updateUser(user)
                else WebActivity.startActivityForAuth(context)
                true
            }
            popup.show()
            true
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
    }

    /**
     * 恢复状态
     * @param savedInstanceState Bundle
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        drawerView.onRestoreInstanceState(savedInstanceState)
    }

    /**
     * 处理回调
     * @param requestCode Int
     * @param resultCode Int
     * @param data Intent?
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            WebActivity.REQUEST_AUTH -> {
                val lastUser = switchUser
                if (resultCode != Activity.RESULT_OK) {
                    userView.setUser(lastUser)
                    userModel.switchToUser(lastUser)
                }
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
        val callUser = user
        collectionCall = Bangumi.getCollectionSax({ user ->
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
            userModel.updateUser(user)
            userModel.switchToUser(user)
            updateUser(user)
        }, {
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
            notify = it
            context.runOnUiThread { context.notifyMenu?.badge = notify?.let { it.first + it.second } ?: 0 }
        }, {
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
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
            if (callUser?.username != this.user?.username) return@buildCallback
            onError(it)
            if ((it as? Exception)?.message == "login failed") {
                user?.let { u -> userModel.removeUser(u) }
                userModel.switchToUser(null)
                updateUser(null)
            }
        }))
    }
}