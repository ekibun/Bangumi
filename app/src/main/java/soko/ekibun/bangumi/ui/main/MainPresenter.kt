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
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper.subscribeOnUiThread
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.api.github.Jsdelivr
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.ResourceUtil

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

    private val user get() = UserModel.current()

    val drawerView = DrawerView(context) {
        user?.let {
            UserModel.removeUser(it)
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
                if (lastUser?.username != "/") drawerView.homeFragment.onUserChange()
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
            UserModel.userList.users.values.forEach {
                popup.menu.add(0, it.user.id, 0, "${it.user.nickname}@${it.user.username}")
            }
            ResourceUtil.checkMenu(context, popup.menu) {
                it.itemId == UserModel.current()?.id
            }
            popup.menu.add("添加账号")
            popup.setOnMenuItemClickListener {
                switchUser = UserModel.current()
                val user = UserModel.userList.users[it.itemId]?.user
                UserModel.switchToUser(user)
                if (lastUser?.username != user?.username) collectionCall?.dispose()
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
                    UserModel.switchToUser(lastUser)
                }
                refreshUser()
            }
        }
    }

    private fun refreshUser() {
        drawerView.homeFragment.updateUserCollection()
    }

    var calendar: List<BangumiCalendarItem> = App.app.dataCacheModel.get("calendar") ?: ArrayList()
    var collectionList: List<Subject> = ArrayList()
    private var collectionCall: Disposable? = null
    var notify: Pair<Int, Int>? = null
    var mergeAirInfo = { list: List<Subject> ->
        list.forEach { subject ->
            val cal = calendar.find { cal -> cal.id == subject.id }
            cal?.eps?.forEach { calEp ->
                subject.eps?.find { ep -> ep.id == calEp.id }?.merge(calEp)
            }
            val eps = (subject.eps as? List<*>)?.mapNotNull { it as? Episode }?.filter { it.type == Episode.TYPE_MAIN }
            val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
            eps?.getOrNull(watchTo?.let { eps.indexOf(it) + 1 } ?: 0)?.let { newEp ->
                if (newEp.airdate == null) return@let
                val use30h = App.app.sp.getBoolean("calendar_use_30h", false)
                val dateTime = cal?.getEpisodeDateTime(newEp) ?: return@let
                val nowInt = CalendarAdapter.getNowInt(use30h)
                subject.airInfo = if (dateTime.first == nowInt) dateTime.second
                else if (dateTime.first > nowInt) {
                    val airDate = CalendarAdapter.getIntCalendar(dateTime.first)
                    val nowDate = CalendarAdapter.getIntCalendar(nowInt)
                    if (airDate.timeInMillis - nowDate.timeInMillis > 24 * 60 * 60 * 1000) "" else "明天${dateTime.second}"
                } else ""
            }
        }
    }

    private fun notifyCollectionChange() {
        context.runOnUiThread {
            drawerView.calendarFragment.onCollectionChange()
            drawerView.homeFragment.collectionFragment.collectionCallback(collectionList, null)
        }
    }

    /**
     * 获取收藏
     */
    fun updateUserCollection() {
        val callUser = user
        collectionCall = Bangumi.getCollectionSax({ user ->
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
            UserModel.updateUser(user)
            UserModel.switchToUser(user)
            updateUser(user)
        }, {
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
            notify = it
            context.notifyMenu?.badge = notify?.let { it.first + it.second } ?: 0
        }).subscribeOnUiThread({
            if (callUser?.username != this.user?.username) throw Exception("Canceled")
            collectionList = it
            mergeAirInfo(it)
            notifyCollectionChange()
        }, {
            if (callUser?.username != this.user?.username) return@subscribeOnUiThread
            drawerView.homeFragment.collectionFragment.collectionCallback(null, it)
            if ((it as? Exception)?.message == "login failed") {
                user?.let { u -> UserModel.removeUser(u) }
                UserModel.switchToUser(null)
                updateUser(null)
            }
        }, key = "update_collection")
    }

    /**
     * 加载日历列表
     */
    fun updateCalendarList() {
        Jsdelivr.createInstance().bangumiCalendar()
            .subscribeOnUiThread({
                calendar = it
                drawerView.calendarFragment.calendarCallback(it, null)
            }, {
                drawerView.calendarFragment.calendarCallback(null, it)
            }, key = "bangumi_calendar")
    }

    init {
        updateCalendarList()
    }
}