package soko.ekibun.bangumi.ui.view

import android.support.v7.app.AppCompatActivity
import android.view.GestureDetector
import android.view.MotionEvent

abstract class SwipeBackActivity: AppCompatActivity() {
    private val detector by lazy{
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val f2 = e1.x - e2.x
                val f1 = f2 / (e1.y - e2.y)
                if (f1 > 3.0f || f1 < -3.0f) {//右滑后退
                    if (f2 <= -160.0f) {
                        finish()
                        return true
                    }
                }
                return false
            }
        })
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return (ev != null && detector.onTouchEvent(ev)) || super.dispatchTouchEvent(ev)
    }
}