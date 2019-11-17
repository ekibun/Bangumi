package soko.ekibun.bangumi.ui.main

import android.view.View
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 用户信息View
 */
class UserView(private val context: MainActivity, onUserFigureClickListener: View.OnClickListener) {
    private val headerView by lazy { context.nav_view.getHeaderView(0) }

    init {
        headerView.user_figure.setOnClickListener(onUserFigureClickListener)
    }

    /**
     * 更新用户信息
     */
    fun setUser(user: UserInfo?) {
        context.runOnUiThread {
            if (context.isDestroyed) return@runOnUiThread
            GlideUtil.with(headerView.user_figure)
                    ?.load(user?.avatar?.large)
                    ?.apply(RequestOptions.placeholderOf(R.drawable.akkarin))
                    ?.apply(RequestOptions.circleCropTransform())
                    ?.into(headerView.user_figure)
            //val token = UserModel(context).getToken()
            headerView.user_id.text = if (user?.username == null) "" else "@${user.username}"
            headerView.user_name.text = user?.nickname ?: context.getString(R.string.hint_login)
            headerView.user_sign.visibility = if (user?.sign.isNullOrEmpty()) View.GONE else View.VISIBLE
            headerView.user_sign.text = user?.sign
        }
    }

}