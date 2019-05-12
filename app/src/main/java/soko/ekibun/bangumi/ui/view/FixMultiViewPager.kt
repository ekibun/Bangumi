package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet

class FixMultiViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : androidx.viewpager.widget.ViewPager(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }
}