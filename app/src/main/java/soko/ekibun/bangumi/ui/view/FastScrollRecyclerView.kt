package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Canvas
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import soko.ekibun.bangumi.R
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import com.simplecityapps.recyclerview_fastscroll.utils.Utils
import kotlin.math.roundToInt

class FastScrollRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : androidx.recyclerview.widget.RecyclerView(context, attrs, defStyleAttr), RecyclerView.OnItemTouchListener {

    private val mScrollbar: FastScroller

    private var mFastScrollEnabled = true

    private var mDownX: Int = 0
    private var mDownY: Int = 0
    private var mLastY: Int = 0

    private var mStateChangeListener: OnFastScrollStateChangeListener? = null

    val scrollBarWidth: Int
        get() = mScrollbar.width

    val scrollBarThumbHeight: Int
        get() = mScrollbar.mThumbHeight

    init {

        val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.FastScrollRecyclerView, 0, 0)
        try {
            mFastScrollEnabled = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollThumbEnabled, true)
        } finally {
            typedArray.recycle()
        }

        mScrollbar = FastScroller(context, this, attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addOnItemTouchListener(this)
    }

    /**
     * We intercept the touch handling only to support fast scrolling when initiated from the
     * scroll bar.  Otherwise, we fall back to the default RecyclerView touch handling.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, ev: MotionEvent): Boolean {
        return handleTouchEvent(ev)
    }

    override fun onTouchEvent(rv: RecyclerView, ev: MotionEvent) {
        handleTouchEvent(ev)
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    private fun handleTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Keep track of the down positions
                mDownX = x
                mLastY = y
                mDownY = mLastY
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
            }
            MotionEvent.ACTION_MOVE -> {
                mLastY = y
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
        }
        return mScrollbar.isDragging
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }

    override fun draw(c: Canvas) {
        super.draw(c)
        if (mFastScrollEnabled) {
            onUpdateScrollbar()
            mScrollbar.draw(c)
        }
    }

    private var itemHeightCache = IntArray(0)
    private fun updateItemHeightCache(){
        val layoutManager = layoutManager?:return
        val adapter = adapter?:return
        if(itemHeightCache.size != adapter.itemCount)
            itemHeightCache = IntArray(adapter.itemCount){200}

        if(layoutManager is LinearLayoutManager){
            val firstIndex= layoutManager.findFirstVisibleItemPosition()
            val lastIndex = layoutManager.findLastVisibleItemPosition()
            for(i in firstIndex..lastIndex)
                itemHeightCache[i] = layoutManager.findViewByPosition(i)?.height?:continue
        }else if(adapter is MeasurableAdapter && layoutManager is StaggeredGridLayoutManager){
            var lastItemHeight = 0
            val spanHeight = IntArray(layoutManager.spanCount) { 0 }
            for(index in 0 until adapter.itemCount){
                val height = adapter.getItemHeight(index)
                val fullspan = adapter.isFullSpan(index)
                val totalOffset = if(fullspan) spanHeight.max()!! else spanHeight.min()!!
                if(fullspan){
                    spanHeight.forEachIndexed { i, _ ->
                        spanHeight[i] = totalOffset + height
                    }
                }else{
                    spanHeight[spanHeight.indexOfFirst { it == totalOffset }] = totalOffset + height
                }
                itemHeightCache[index] = totalOffset + height - lastItemHeight
                lastItemHeight = totalOffset + height
            }
        }
    }
    var nestedScrollRange = {0}
    var nestedScrollDistance = {0}
    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    fun scrollToPositionAtProgress(scrollBarY: Float): String {
        updateItemHeightCache()

        val nestedRange = nestedScrollRange()
        val totalHeight = nestedRange + itemHeightCache.sum()

        val scrollRange = height - paddingBottom - nestedRange - scrollTopMargin
        mScrollbar.mThumbHeight = Math.max(height * scrollRange / totalHeight, mScrollbar.minThumbHeight)

        val availableScrollHeight = totalHeight - height + paddingBottom
        val availableScrollBarHeight = scrollRange - mScrollbar.mThumbHeight

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            return ""
        }

        val nestedDistance = nestedScrollDistance()
        val scrolledPastHeight = getScrolledPastHeight()
        val lastScrollBarY = (scrolledPastHeight + nestedDistance).toFloat() / availableScrollHeight * availableScrollBarHeight + nestedDistance + scrollTopMargin
        var dy = (scrollBarY - lastScrollBarY) * availableScrollHeight/ availableScrollBarHeight
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        val consumed = IntArray(2)
        val offsetInWindow = IntArray(2)
        dispatchNestedPreScroll(0, dy.roundToInt(), consumed, offsetInWindow)
        dy -= consumed[1].toFloat()
        val scrollY = Math.min(Math.max(scrolledPastHeight + dy, 0f), availableScrollHeight.toFloat())
        dy -= scrollY - scrolledPastHeight
        dispatchNestedScroll(consumed[0], consumed[1], 0, dy.roundToInt(), offsetInWindow)

        val layoutManager = layoutManager
        val adapter = adapter
        var totalOffset = 0
        itemHeightCache.forEachIndexed { index, height ->
            if(scrollY >= totalOffset && scrollY<=totalOffset + height){
                if(layoutManager is LinearLayoutManager)
                    layoutManager.scrollToPositionWithOffset(index, totalOffset - scrollY.roundToInt())
                else if(adapter is MeasurableAdapter && layoutManager is androidx.recyclerview.widget.StaggeredGridLayoutManager)
                    layoutManager.scrollToPositionWithOffset(index, totalOffset - scrollY.roundToInt())
                val sectionedAdapter = (adapter as? SectionedAdapter)?:return ""
                return sectionedAdapter.getSectionName(index)
            }
            totalOffset += height
        }
        return ""
    }

    private fun getScrolledPastHeight(): Int{
        val layoutManager = layoutManager
        val adapter = adapter
        return if(layoutManager is LinearLayoutManager){
            val firstIndex= layoutManager.findFirstVisibleItemPosition()
            val topOffset = layoutManager.getDecoratedTop(layoutManager.findViewByPosition(firstIndex)?:return 0)
            itemHeightCache.sliceArray(0 until firstIndex).sum() - topOffset
        } else if(adapter is MeasurableAdapter && layoutManager is StaggeredGridLayoutManager){
            val firstIndex = layoutManager.findFirstVisibleItemPositions(null)[0]
            val topOffset = layoutManager.getDecoratedTop(layoutManager.findViewByPosition(firstIndex)?:return 0)
            itemHeightCache.sliceArray(0 until firstIndex).sum() - topOffset
        } else 0
    }
    /**
     * Updates the bounds for the scrollbar.
     */
    var scrollTopMargin = 0
    private fun onUpdateScrollbar() {
        updateItemHeightCache()

        val nestedRange = nestedScrollRange()
        val totalHeight = nestedRange + itemHeightCache.sum()

        val scrollRange = height - paddingBottom - nestedRange - scrollTopMargin
        mScrollbar.mThumbHeight = Math.max(height * scrollRange / totalHeight, mScrollbar.minThumbHeight)

        val availableScrollHeight = totalHeight - height + paddingBottom

        val scrolledPastHeight: Int = getScrolledPastHeight()


        val availableScrollBarHeight = scrollRange - mScrollbar.mThumbHeight

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbPosition(-1, -1)
            return
        }
        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        val nestedY = nestedScrollDistance()
        val scrollY = scrolledPastHeight + nestedY
        val scrollBarY = scrollTopMargin + nestedY + (scrollY.toFloat() / availableScrollHeight * availableScrollBarHeight).toInt()

        // Calculate the position and size of the scroll bar
        val scrollBarX: Int = if (Utils.isRtl(resources)) {
            0
        } else {
            width - mScrollbar.width
        }
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY)
    }

    interface SectionedAdapter {
        fun getSectionName(position: Int): String
    }

    interface MeasurableAdapter {
        fun isFullSpan(position: Int): Boolean
        fun getItemHeight(position: Int): Int
    }
}
