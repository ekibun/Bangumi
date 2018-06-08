@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.main

import android.app.ProgressDialog
import android.content.DialogInterface
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo

class UserView(private val context: MainActivity, onUserFigureClickListener: View.OnClickListener){
    private val headerView by lazy{context.nav_view.getHeaderView(0)}

    init{
        headerView.user_figure.setOnClickListener(onUserFigureClickListener)
    }

    fun setUser(user: UserInfo?){
        context.runOnUiThread {
            if(context.isDestroyed) return@runOnUiThread
            Glide.with(headerView)
                    .load(user?.avatar?.large)
                    .apply(RequestOptions.placeholderOf(R.drawable.akkarin))
                    .into(headerView.user_figure)
            headerView.user_id.text = if(user?.id == null) "" else "@${user.id}"
            headerView.user_name.text = user?.nickname
                    ?:context.getString(R.string.login_hint)
        }
    }

    fun createLoginProgressDialog(listener: DialogInterface.OnCancelListener): ProgressDialog{
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage(context.getString(R.string.loging))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setOnCancelListener(listener)
        return progressDialog
    }

}