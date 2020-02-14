package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class FixSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwipeRefreshLayout(context, attrs) {
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    override fun setRefreshing(refreshing: Boolean) {
        clearAnimation()
        super.setRefreshing(refreshing)
    }

    var prevX = 0f
    var declined = false
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = ev.x
                declined = false
            }
            MotionEvent.ACTION_MOVE -> if (declined || Math.abs(ev.x - prevX) > touchSlop) {
                declined = true
                return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}