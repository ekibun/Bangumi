package soko.ekibun.bangumi.ui.view

import android.view.GestureDetector
import android.view.MotionEvent

/**
 * 滑动返回View
 */
abstract class SwipeBackActivity: BaseActivity() {
    private val detector by lazy{
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val f2 = e2.x - e1.x
                val f1 = f2 / (e2.y - e1.y)
                if ((e1.x < 100f || shouldCancelActivity) && Math.abs(f1) > 3.0f && f2 > 160f && velocityX > 300f) {
                    processBack()
                    return true
                }
                return false
            }
        })
    }
    var shouldCancelActivity = false
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if(ev?.action == MotionEvent.ACTION_DOWN)
            shouldCancelActivity = true
        return (ev != null && detector.onTouchEvent(ev)) || super.dispatchTouchEvent(ev)
    }

    /**
     * 返回处理
     */
    open fun processBack(){
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}