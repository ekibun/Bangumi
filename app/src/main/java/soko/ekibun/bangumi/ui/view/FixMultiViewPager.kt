package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Author :  suzeyu
 * Time   :  2016-12-26  上午1:41
 * ClassDescription : 对多点触控场景时, {@link android.support.v4.view.ViewPager#onInterceptTouchEvent(MotionEvent)}中
 *                  pointerIndex = -1. 发生IllegalArgumentException: pointerIndex out of range 处理
 */
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