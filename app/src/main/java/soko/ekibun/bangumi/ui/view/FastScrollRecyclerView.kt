package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.entity.AbstractExpandableItem
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.ResourceUtil
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

    private fun getRealItemIndex(adapterItemIndex: Int): Int {
        val adapter = (adapter as? BaseQuickAdapter<*, *>) ?: return adapterItemIndex
        var cur = 0
        for (index in 0..adapterItemIndex) {
            val item = adapter.data.getOrNull(index) ?: return cur
            if (item is AbstractExpandableItem<*> && item.level > 0) continue
            val subItemCount = if (item is AbstractExpandableItem<*> && item.isExpanded) item.subItems?.size ?: 0 else 0
            if (index + 1 + subItemCount > adapterItemIndex)
                return cur + (adapterItemIndex - index)
            cur += 1 + subItemCount
        }
        return cur
    }

    private fun getAdapterItemIndex(realItemIndex: Int): Int {
        val adapter = (adapter as? BaseQuickAdapter<*, *>) ?: return realItemIndex
        var cur = 0
        for (index in 0 until adapter.itemCount) {
            val item = adapter.data[index]
            if (item is AbstractExpandableItem<*> && item.level > 0) continue
            val subItemCount = if (item is AbstractExpandableItem<*> && item.isExpanded) item.subItems?.size ?: 0 else 0
            if (cur + 1 + subItemCount > realItemIndex)
                return index + (realItemIndex - cur)
            cur += 1 + subItemCount
        }
        return realItemIndex
    }

    private var itemHeightCache = IntArray(0)
    private fun updateItemHeightCache() {
        val layoutManager = layoutManager ?: return
        val adapter = adapter ?: return
        val itemCount = ((adapter as? BaseQuickAdapter<*, *>)?.data?.filter { it !is AbstractExpandableItem<*> || it.level == 0 }
                ?.map { 1 + if (it is AbstractExpandableItem<*> && it.isExpanded) it.subItems?.size ?: 0 else 0 }?.sum()
                ?: 0) + 1
        if (itemHeightCache.size != itemCount)
            itemHeightCache = IntArray(itemCount) { itemHeightCache.getOrNull(it) ?: 200 }

        if (layoutManager is LinearLayoutManager) {
            val firstIndex = layoutManager.findFirstVisibleItemPosition()
            val lastIndex = layoutManager.findLastVisibleItemPosition()
            for (i in firstIndex..lastIndex)
                itemHeightCache[getRealItemIndex(i)] = layoutManager.findViewByPosition(i)?.height ?: continue
        } else if (adapter is MeasurableAdapter && layoutManager is StaggeredGridLayoutManager) {
            var lastItemHeight = 0
            val spanHeight = IntArray(layoutManager.spanCount) { 0 }
            for (index in 0 until itemCount) {
                val warpIndex = getAdapterItemIndex(index)
                val height = adapter.getItemHeight(warpIndex)
                val fullspan = adapter.isFullSpan(warpIndex)
                val totalOffset = if (fullspan) spanHeight.max()!! else spanHeight.min()!!
                if (fullspan) {
                    spanHeight.forEachIndexed { i, _ ->
                        spanHeight[i] = totalOffset + height
                    }
                } else {
                    spanHeight[spanHeight.indexOfFirst { it == totalOffset }] = totalOffset + height
                }
                itemHeightCache[index] = totalOffset + height - lastItemHeight
                lastItemHeight = totalOffset + height
            }
        }
    }

    var nestedScrollRange = { 0 }
    var nestedScrollDistance = { 0 }
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
        var dy = (scrollBarY - lastScrollBarY) * availableScrollHeight / availableScrollBarHeight
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        val consumed = IntArray(2)
        val offsetInWindow = IntArray(2)
        dispatchNestedPreScroll(0, dy.roundToInt(), consumed, offsetInWindow)
        dy -= consumed[1].toFloat()
        val scrollY = Math.min(Math.max(scrolledPastHeight + dy, 0f), availableScrollHeight.toFloat())
        dy -= scrollY - scrolledPastHeight
        dispatchNestedScroll(consumed[0], consumed[1], 0, Math.min(nestedDistance, dy.roundToInt()), offsetInWindow)

        val layoutManager = layoutManager
        val adapter = adapter
        var totalOffset = 0
        itemHeightCache.forEachIndexed { index, height ->
            if (scrollY >= totalOffset && scrollY <= totalOffset + height) {
                val wrapIndex = getAdapterItemIndex(index)
                if (layoutManager is LinearLayoutManager)
                    layoutManager.scrollToPositionWithOffset(wrapIndex, totalOffset - scrollY.roundToInt())
                else if (adapter is MeasurableAdapter && layoutManager is StaggeredGridLayoutManager)
                    layoutManager.scrollToPositionWithOffset(wrapIndex, totalOffset - scrollY.roundToInt())
                val sectionedAdapter = (adapter as? SectionedAdapter) ?: return ""
                return sectionedAdapter.getSectionName(wrapIndex)
            }
            totalOffset += height
        }
        return ""
    }

    private fun getScrolledPastHeight(): Int {
        val layoutManager = layoutManager
        val adapter = adapter
        return if (layoutManager is LinearLayoutManager) {
            val firstIndex = layoutManager.findFirstVisibleItemPosition()
            val topOffset = layoutManager.getDecoratedTop(layoutManager.findViewByPosition(firstIndex) ?: return 0)
            itemHeightCache.sliceArray(0 until getRealItemIndex(firstIndex)).sum() - topOffset
        } else if (adapter is MeasurableAdapter && layoutManager is StaggeredGridLayoutManager) {
            val firstIndex = layoutManager.findFirstVisibleItemPositions(null)[0]
            val topOffset = layoutManager.getDecoratedTop(layoutManager.findViewByPosition(firstIndex) ?: return 0)
            itemHeightCache.sliceArray(0 until getRealItemIndex(firstIndex)).sum() - topOffset
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
        val nestedDistance = nestedScrollDistance()
        val scrollBarY = (scrolledPastHeight + nestedDistance).toFloat() / availableScrollHeight * availableScrollBarHeight + nestedDistance + scrollTopMargin

        // Calculate the position and size of the scroll bar
        val scrollBarX: Int = if (ResourceUtil.isRtl(resources)) {
            0
        } else {
            width - mScrollbar.width
        }
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY.toInt())
    }

    interface SectionedAdapter {
        fun getSectionName(position: Int): String
    }

    interface MeasurableAdapter {
        fun isFullSpan(position: Int): Boolean
        fun getItemHeight(position: Int): Int
    }
}
