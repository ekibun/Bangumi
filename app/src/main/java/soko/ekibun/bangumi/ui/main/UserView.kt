package soko.ekibun.bangumi.ui.main

import android.view.View
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 用户信息View
 * @property context MainActivity
 * @property headerView (android.view.View..android.view.View?)
 * @constructor
 */
class UserView(private val context: MainActivity, onUserFigureClickListener: View.OnClickListener) {
    val headerView by lazy { context.nav_view.getHeaderView(0) }

    init {
        headerView.user_figure.setOnClickListener(onUserFigureClickListener)
        setUser(UserModel.current())
    }

    /**
     * 更新用户信息
     * @param user UserInfo?
     */
    fun setUser(user: UserInfo?) {
        if (context.isDestroyed) return
        GlideUtil.with(headerView.user_figure)
            ?.load(Images.large(user?.avatar))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.akkarin).placeholder(R.drawable.placeholder_round)
            )
            ?.into(headerView.user_figure)
        headerView.user_id.text = if (user?.username == null) "" else "@${user.username}"
        headerView.user_name.text = user?.nickname ?: context.getString(R.string.hint_login)
        headerView.user_sign.visibility = if (user?.sign.isNullOrEmpty()) View.GONE else View.VISIBLE
        headerView.user_sign.text = user?.sign
    }

}