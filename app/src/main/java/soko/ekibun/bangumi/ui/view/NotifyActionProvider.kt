package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ActionProvider
import soko.ekibun.bangumi.R

/**
 * 通知提示ActionProvider
 * @property mIvIcon ImageView?
 * @property mTvBadge TextView?
 * @property onClick Function0<Unit>
 * @property badge Int
 * @constructor
 */
class NotifyActionProvider(context: Context): ActionProvider(context){
    private var mIvIcon: ImageView? = null
    private var mTvBadge: TextView? = null

    var onClick = {}

    @SuppressLint("InflateParams", "PrivateResource")
    override fun onCreateActionView(): View {
        val size = context.resources.getDimensionPixelSize(
                com.google.android.material.R.dimen.abc_action_bar_default_height_material)

        val layoutParams = ViewGroup.LayoutParams(size, size)
        val view = LayoutInflater.from(context)
                .inflate(R.layout.action_notify, null, false)
        view.layoutParams = layoutParams
        mIvIcon = view.findViewById(R.id.iv_icon) as ImageView
        mTvBadge = view.findViewById(R.id.tv_badge) as TextView
        view.setOnClickListener{ onClick() }
        updateBadge(badge)
        return view
    }

    var badge = 0
        set(value) {
            field = value
            updateBadge(value)
        }
    private fun updateBadge(i: Int){
        mTvBadge?.text = i.toString()
        mTvBadge?.visibility = if(i == 0) View.INVISIBLE else View.VISIBLE
        mIvIcon?.setImageResource(if(i==0) R.drawable.ic_notifications_none else R.drawable.ic_notifications)
    }

}