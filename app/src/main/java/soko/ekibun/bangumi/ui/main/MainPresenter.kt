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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 主页Presenter
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
        Log.v("updateUser", "${lastUser?.username}->${user?.username}")
        if (lastUser?.username != user?.username) {
            collectionList = ArrayList()
            if (lastUser?.username != "/" || user == null) drawerView.homeFragment.onUserChange()
        }
        lastUser = user
        context.nav_view.menu.findItem(R.id.nav_logout).isVisible = user != null
        userView.setUser(user)
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
                if (lastUser?.username != user?.username) context.disposeContainer.dispose(COLLECTION_CALL)
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

    var calendar: List<BangumiCalendarItem> = App.app.dataCacheModel.get(CALENDAR_CACHE_KEY) ?: ArrayList()
    var collectionList: List<Subject> = ArrayList()
    var notify: Pair<Int, Int>? = null

    private fun notifyCollectionChange() {
        drawerView.calendarFragment.onCollectionChange()
        drawerView.homeFragment.collectionFragment.collectionCallback(collectionList, null)
    }

    /**
     * 获取收藏
     */
    fun updateUserCollection() {
        val callUser = user

        context.disposeContainer.subscribeOnUiThread(
            Bangumi.getCollectionSax(),
            { data ->
                if (callUser?.username != this.user?.username) return@subscribeOnUiThread
                when (data) {
                    is UserInfo -> {
                        UserModel.updateUser(data)
                        UserModel.switchToUser(data)
                        updateUser(data)
                    }
                    is Pair<*, *> -> {
                        val notify = Pair(data.first as Int, data.second as Int)
                        this.notify = notify
                        context.notifyMenu?.badge = notify.let { it.first + it.second }
                    }
                    is List<*> -> {
                        mergeAirInfo(data.map { it as Subject })
                    }
                }
            }, {
                if (callUser?.username != this.user?.username) return@subscribeOnUiThread
                drawerView.homeFragment.collectionFragment.collectionCallback(null, it)
                if ((it as? Exception)?.message == "login failed") {
                    user?.let { u -> UserModel.removeUser(u) }
                    UserModel.switchToUser(null)
                    updateUser(null)
                }
            },
            key = COLLECTION_CALL
        )
    }

    var mergeAirInfo = { }

    private fun mergeAirInfo(collection: List<Subject>) {
        val calendar = calendar
        context.disposeContainer.subscribeOnUiThread(
            Observable.just(0).observeOn(Schedulers.computation()).map {
                collection.forEach { subject ->
                    val cal = calendar.find { cal -> cal.id == subject.id }
                    cal?.eps?.forEach { calEp ->
                        subject.eps?.find { ep -> ep.id == calEp.id }?.merge(calEp)
                    }
                    val eps = (subject.eps as? List<*>)?.mapNotNull { it as? Episode }
                        ?.filter { it.type == Episode.TYPE_MAIN }
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
            },
            {
                collectionList = collection
                mergeAirInfo()
                notifyCollectionChange()
            },
            key = "bangumi_collection_merge"
        )
    }

    /**
     * 加载日历列表
     */
    fun updateCalendarList() {
        context.disposeContainer.subscribeOnUiThread(
            Github.bangumiCalendar(),
            {
                calendar = it
                App.app.dataCacheModel.set(CALENDAR_CACHE_KEY, it)
                drawerView.calendarFragment.calendarCallback(it, null)
            }, {
                drawerView.calendarFragment.calendarCallback(null, it)
            },
            key = "bangumi_calendar"
        )
    }

    init {
        updateCalendarList()
    }

    companion object {
        const val COLLECTION_CALL = "bangumi_collection"

        const val CALENDAR_CACHE_KEY = "calendar"
    }

}