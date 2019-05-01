package soko.ekibun.bangumi.ui.view

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent

class DragSelectTouchListener : RecyclerView.OnItemTouchListener {

    private var isActive: Boolean = false
    private var start: Int = 0
    private var end: Int = 0

    var selectListener: ((start: Int, end: Int, isSelected: Boolean)->Unit)? = null

    private var recyclerView: RecyclerView? = null

    private val autoScrollDistance = (Resources.getSystem().displayMetrics.density * 56).toInt()

    private var mTopBound: Int = 0
    private var mBottomBound: Int = 0

    private var inTopSpot: Boolean = false
    private var inBottomSpot: Boolean = false

    private val autoScrollHandler = Handler(Looper.getMainLooper())

    private var scrollDistance: Int = 0

    private var lastX: Float = 0.toFloat()
    private var lastY: Float = 0.toFloat()

    private var lastStart: Int = 0
    private var lastEnd: Int = 0

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (!inTopSpot && !inBottomSpot) return
            scrollBy(scrollDistance)
            autoScrollHandler.postDelayed(this, DELAY.toLong())
        }
    }

    private val scrollRun = object : Runnable {
        override fun run() {
            scrollBy(scrollDistance)
            ViewCompat.postOnAnimation(recyclerView!!, this)
        }
    }

    init {
        reset()
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isActive || rv.adapter?.itemCount == 0) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN -> {
                reset()
            }
        }
        recyclerView = rv
        mTopBound = -20
        mBottomBound = rv.height - autoScrollDistance - nestScrollDistance()
        return true
    }

    var nestScrollDistance = {0}

    private fun startAutoScroll() {
        recyclerView?.let{
            it.removeCallbacks(scrollRun)
            ViewCompat.postOnAnimation(it, scrollRun)
        }
    }

    private fun stopAutoScroll() {
        recyclerView?.removeCallbacks(scrollRun)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isActive) return
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!inTopSpot && !inBottomSpot) //更新滑动选择区域
                    updateSelectedRange(rv, e)
                //在顶部或者底部触发自动滑动
                processAutoScroll(e)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
                //结束滑动选择，初始化各状态值
                reset()
        }
    }

    private fun updateSelectedRange(rv: RecyclerView, e: MotionEvent) {
        updateSelectedRange(rv, e.x, e.y)
    }

    private fun updateSelectedRange(rv: RecyclerView, x: Float, y: Float) {
        val child = rv.findChildViewUnder(x, y)
        if (child != null) {
            val position = rv.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && end != position) {
                end = position
                notifySelectRangeChange()
            }
        }
    }


    private fun processAutoScroll(event: MotionEvent) {
        val y = event.y.toInt()
        if (y < mTopBound) {
            lastX = event.x
            lastY = event.y
            scrollDistance = -(mTopBound - y) / SCROLL_FACTOR
            if (!inTopSpot) {
                inTopSpot = true
                //                autoScrollHandler.removeCallbacks(scrollRunnable);
                //                autoScrollHandler.postDelayed(scrollRunnable, DELAY);
                startAutoScroll()
            }
        } else if (y > mBottomBound) {
            lastX = event.x
            lastY = event.y
            scrollDistance = (y - mBottomBound) / SCROLL_FACTOR
            if (!inBottomSpot) {
                inBottomSpot = true
                //                autoScrollHandler.removeCallbacks(scrollRunnable);
                //                autoScrollHandler.postDelayed(scrollRunnable, DELAY);
                startAutoScroll()
            }
        } else {
            //            autoScrollHandler.removeCallbacks(scrollRunnable);
            inBottomSpot = false
            inTopSpot = false
            lastX = java.lang.Float.MIN_VALUE
            lastY = java.lang.Float.MIN_VALUE
            stopAutoScroll()
        }
    }

    private fun notifySelectRangeChange() {
        if (selectListener == null) {
            return
        }
        if (start == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION) {
            return
        }

        val newStart: Int = Math.min(start, end)
        val newEnd: Int = Math.max(start, end)
        if (lastStart == RecyclerView.NO_POSITION || lastEnd == RecyclerView.NO_POSITION) {
            if (newEnd - newStart == 1) {
                selectListener?.invoke(newStart, newStart, true)
            } else {
                selectListener?.invoke(newStart, newEnd, true)
            }
        } else {
            if (newStart > lastStart) {
                selectListener?.invoke(lastStart, newStart - 1, false)
            } else if (newStart < lastStart) {
                selectListener?.invoke(newStart, lastStart - 1, true)
            }

            if (newEnd > lastEnd) {
                selectListener?.invoke(lastEnd + 1, newEnd, true)
            } else if (newEnd < lastEnd) {
                selectListener?.invoke(newEnd + 1, lastEnd, false)
            }
        }

        lastStart = newStart
        lastEnd = newEnd
    }

    private fun reset() {
        setIsActive(false)
        start = RecyclerView.NO_POSITION
        end = RecyclerView.NO_POSITION
        lastStart = RecyclerView.NO_POSITION
        lastEnd = RecyclerView.NO_POSITION
        autoScrollHandler.removeCallbacks(scrollRunnable)
        inTopSpot = false
        inBottomSpot = false
        lastX = java.lang.Float.MIN_VALUE
        lastY = java.lang.Float.MIN_VALUE
        stopAutoScroll()
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }

    private fun scrollBy(distance: Int) {
        val scrollDistance: Int = if (distance > 0) {
            Math.min(distance, MAX_SCROLL_DISTANCE)
        } else {
            Math.max(distance, -MAX_SCROLL_DISTANCE)
        }
        recyclerView?.let{
            it.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            val consumed = IntArray(2)
            val offsetInWindow = IntArray(2)
            it.dispatchNestedPreScroll(0, scrollDistance, consumed, offsetInWindow)
            it.dispatchNestedScroll(consumed[0], consumed[1], 0, 0, offsetInWindow)
            it.scrollBy(0, scrollDistance-consumed[1])
        }
        if (lastX != java.lang.Float.MIN_VALUE && lastY != java.lang.Float.MIN_VALUE) {
            updateSelectedRange(recyclerView!!, lastX, lastY)
        }
    }

    private fun setIsActive(isActive: Boolean) {
        this.isActive = isActive
    }

    fun setStartSelectPosition(position: Int) {
        setIsActive(true)
        start = position
        end = position
        lastStart = position
        lastEnd = position
    }

    companion object {

        private const val DELAY = 25

        private const val MAX_SCROLL_DISTANCE = 16

        //这个数越大，滚动的速度增加越慢
        private const val SCROLL_FACTOR = 6
    }
}