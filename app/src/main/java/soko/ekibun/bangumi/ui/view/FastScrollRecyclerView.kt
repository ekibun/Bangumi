package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.support.annotation.ColorInt
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import com.simplecityapps.recyclerview_fastscroll.R
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import com.simplecityapps.recyclerview_fastscroll.utils.Utils

class FastScrollRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr), RecyclerView.OnItemTouchListener {

    private val mScrollbar: FastScroller

    private var mFastScrollEnabled = true

    private val mScrollPosState = ScrollPositionState()

    private var mDownX: Int = 0
    private var mDownY: Int = 0
    private var mLastY: Int = 0

    private val mScrollOffsets: SparseIntArray

    private val mScrollOffsetInvalidator: ScrollOffsetInvalidator
    private var mStateChangeListener: OnFastScrollStateChangeListener? = null

    val scrollBarWidth: Int
        get() = mScrollbar.width

    val scrollBarThumbHeight: Int
        get() = mScrollbar.thumbHeight

    /**
     * Returns the available scroll bar height:
     * AvailableScrollBarHeight = Total height of the visible view - thumb height
     */
    fun getAvailableScrollBarHeight(): Int{
        val visibleHeight = height
        return visibleHeight - mScrollbar.thumbHeight
    }

    /**
     * The current scroll state of the recycler view.  We use this in onUpdateScrollbar()
     * and scrollToPositionAtProgress() to determine the scroll position of the recycler view so
     * that we can calculate what the scroll bar looks like, and where to jump to from the fast
     * scroller.
     */
    class ScrollPositionState {
        // The index of the first visible row
        internal var rowIndex: Int = 0
        // The offset of the first visible row
        internal var rowTopOffset: Int = 0
        // The height of a given row (they are currently all the same height)
        internal var rowHeight: Int = 0
    }

    init {

        val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.FastScrollRecyclerView, 0, 0)
        try {
            mFastScrollEnabled = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollThumbEnabled, true)
        } finally {
            typedArray.recycle()
        }

        mScrollbar = FastScroller(context, this, attrs)
        mScrollOffsetInvalidator = ScrollOffsetInvalidator()
        mScrollOffsets = SparseIntArray()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addOnItemTouchListener(this)
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        if (getAdapter() != null) {
            getAdapter()!!.unregisterAdapterDataObserver(mScrollOffsetInvalidator)
        }

        adapter?.registerAdapterDataObserver(mScrollOffsetInvalidator)

        super.setAdapter(adapter)
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

    /**
     * Returns the available scroll height:
     * AvailableScrollHeight = Total height of the all items - last page height
     *
     * @param yOffset the offset from the top of the recycler view to start tracking.
     */
    protected fun getAvailableScrollHeight(adapterHeight: Int, yOffset: Int): Int {
        val visibleHeight = height
        val scrollHeight = paddingTop + yOffset + adapterHeight + paddingBottom
        return scrollHeight - visibleHeight
    }

    override fun draw(c: Canvas) {
        super.draw(c)
        if (mFastScrollEnabled) {
            onUpdateScrollbar()
            mScrollbar.draw(c)
        }
    }

    /**
     * Updates the scrollbar thumb offset to match the visible scroll of the recycler view.  It does
     * this by mapping the available scroll area of the recycler view to the available space for the
     * scroll bar.
     * @param scrollPosState the current scroll position
     * @param rowCount       the number of rows, used to calculate the total scroll height (assumes that
     */
    fun updateThumbPosition(scrollPosState: ScrollPositionState, rowCount: Int) {
        val availableScrollHeight: Int
        val scrolledPastHeight: Int
        if (adapter is MeasurableAdapter) {
            availableScrollHeight = getAvailableScrollHeight(calculateAdapterHeight(), 0)
            scrolledPastHeight = calculateScrollDistanceToPosition(scrollPosState.rowIndex)
        } else {
            availableScrollHeight = getAvailableScrollHeight(rowCount * scrollPosState.rowHeight, 0)
            scrolledPastHeight = scrollPosState.rowIndex * scrollPosState.rowHeight
        }

        val availableScrollBarHeight = getAvailableScrollBarHeight()

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbPosition(-1, -1)
            return
        }

        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        val scrollY = paddingTop + scrolledPastHeight - scrollPosState.rowTopOffset
        val scrollBarY = (scrollY.toFloat() / availableScrollHeight * availableScrollBarHeight).toInt()

        // Calculate the position and size of the scroll bar
        val scrollBarX: Int
        if (Utils.isRtl(resources)) {
            scrollBarX = 0
        } else {
            scrollBarX = width - mScrollbar.width
        }
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY)
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    fun scrollToPositionAtProgress(touchFraction: Float): String {
        val itemCount = adapter!!.itemCount
        if (itemCount == 0) {
            return ""
        }
        var spanCount = 1
        var rowCount = itemCount
        if (layoutManager is GridLayoutManager) {
            spanCount = (layoutManager as GridLayoutManager).spanCount
            rowCount = Math.ceil(rowCount.toDouble() / spanCount).toInt()
        }

        // Stop the scroller if it is scrolling
        stopScroll()

        getCurScrollState(mScrollPosState)

        val itemPos: Float
        val availableScrollHeight: Int

        val scrollPosition: Int
        val scrollOffset: Int

        if (adapter is MeasurableAdapter) {
            itemPos = findItemPosition(touchFraction)
            availableScrollHeight = calculateAdapterHeight()
            scrollPosition = itemPos.toInt()
            scrollOffset = calculateScrollDistanceToPosition(scrollPosition) - (touchFraction * availableScrollHeight).toInt()
        } else {
            itemPos = findItemPosition(touchFraction)
            availableScrollHeight = getAvailableScrollHeight(rowCount * mScrollPosState.rowHeight, 0)

            //The exact position of our desired item
            val exactItemPos = (availableScrollHeight * touchFraction).toInt()

            //The offset used here is kind of hard to explain.
            //If the position we wish to scroll to is, say, position 10.5, we scroll to position 10,
            //and then offset by 0.5 * rowHeight. This is how we achieve smooth scrolling.
            scrollPosition = spanCount * exactItemPos / mScrollPosState.rowHeight
            scrollOffset = -(exactItemPos % mScrollPosState.rowHeight)
        }

        val layoutManager = layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset)

        if (adapter !is SectionedAdapter) {
            return ""
        }

        val posInt = (if (touchFraction == 1f) itemPos - 1 else itemPos).toInt()

        val sectionedAdapter = adapter as SectionedAdapter?
        return sectionedAdapter!!.getSectionName(posInt)
    }

    private fun findItemPosition(touchFraction: Float): Float {

        if (adapter is MeasurableAdapter) {
            val measurer = adapter as MeasurableAdapter
            val viewTop = (touchFraction * calculateAdapterHeight()).toInt()

            for (i in 0 until adapter!!.itemCount) {
                val top = calculateScrollDistanceToPosition(i)
                val bottom = top + measurer.getViewHeight(this, findViewHolderForAdapterPosition(i) as Nothing?, i)
                if (viewTop in top..bottom) {
                    return i.toFloat()
                }
            }

            // Should never happen
            Log.w(TAG, "Failed to find a view at the provided scroll fraction ($touchFraction)")
            return touchFraction * adapter!!.itemCount
        } else {
            return adapter!!.itemCount * touchFraction
        }
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    fun onUpdateScrollbar() {

        if (adapter == null) {
            return
        }

        var rowCount = adapter!!.itemCount
        if (layoutManager is GridLayoutManager) {
            val spanCount = (layoutManager as GridLayoutManager).spanCount
            rowCount = Math.ceil(rowCount.toDouble() / spanCount).toInt()
        }
        // Skip early if, there are no items.
        if (rowCount == 0) {
            mScrollbar.setThumbPosition(-1, -1)
            return
        }

        // Skip early if, there no child laid out in the container.
        getCurScrollState(mScrollPosState)
        if (mScrollPosState.rowIndex < 0) {
            mScrollbar.setThumbPosition(-1, -1)
            return
        }

        updateThumbPosition(mScrollPosState, rowCount)
    }

    /**
     * Returns the current scroll state of the apps rows.
     */
    private fun getCurScrollState(stateOut: ScrollPositionState) {
        stateOut.rowIndex = -1
        stateOut.rowTopOffset = -1
        stateOut.rowHeight = -1

        val itemCount = adapter!!.itemCount

        // Return early if there are no items, or no children.
        if (itemCount == 0 || childCount == 0) {
            return
        }

        val child = getChildAt(0)

        stateOut.rowIndex = getChildAdapterPosition(child)
        if (layoutManager is GridLayoutManager) {
            stateOut.rowIndex = stateOut.rowIndex / (layoutManager as GridLayoutManager).spanCount
        }
        stateOut.rowTopOffset = layoutManager!!.getDecoratedTop(child)
        stateOut.rowHeight = (child.height + layoutManager!!.getTopDecorationHeight(child)
                + layoutManager!!.getBottomDecorationHeight(child))
    }

    /**
     * Calculates the total height of all views above a position in the recycler view. This method
     * should only be called when the attached adapter implements [MeasurableAdapter].
     *
     * @param adapterIndex The index in the adapter to find the total height above the
     * corresponding view
     * @return The total height of all views above `adapterIndex` in pixels
     */
    private fun calculateScrollDistanceToPosition(adapterIndex: Int): Int {
        if (adapter !is MeasurableAdapter) {
            throw IllegalStateException("calculateScrollDistanceToPosition() should only be called where the RecyclerView.Adapter is an instance of MeasurableAdapter")
        }

        if (mScrollOffsets.indexOfKey(adapterIndex) >= 0) {
            return mScrollOffsets.get(adapterIndex)
        }

        var totalHeight = 0
        val measurer = adapter as MeasurableAdapter

        // TODO Take grid layouts into account

        for (i in 0 until adapterIndex) {
            mScrollOffsets.put(i, totalHeight)
            totalHeight += measurer.getViewHeight(this, findViewHolderForAdapterPosition(i), i)
        }

        mScrollOffsets.put(adapterIndex, totalHeight)
        return totalHeight
    }

    /**
     * Calculates the total height of the recycler view. This method should only be called when the
     * attached adapter implements [MeasurableAdapter].
     *
     * @return The total height of all rows in the RecyclerView
     */
    private fun calculateAdapterHeight(): Int {
        if (adapter !is MeasurableAdapter) {
            throw IllegalStateException("calculateAdapterHeight() should only be called where the RecyclerView.Adapter is an instance of MeasurableAdapter")
        }
        return calculateScrollDistanceToPosition(adapter!!.itemCount)
    }

    fun showScrollbar() {
        mScrollbar.show()
    }

    fun setThumbColor(@ColorInt color: Int) {
        mScrollbar.setThumbColor(color)
    }

    fun setTrackColor(@ColorInt color: Int) {
        mScrollbar.setTrackColor(color)
    }

    fun setPopupBgColor(@ColorInt color: Int) {
        mScrollbar.setPopupBgColor(color)
    }

    fun setPopupTextColor(@ColorInt color: Int) {
        mScrollbar.setPopupTextColor(color)
    }

    fun setPopupTextSize(textSize: Int) {
        mScrollbar.setPopupTextSize(textSize)
    }

    fun setPopUpTypeface(typeface: Typeface) {
        mScrollbar.setPopupTypeface(typeface)
    }

    fun setAutoHideDelay(hideDelay: Int) {
        mScrollbar.setAutoHideDelay(hideDelay)
    }

    fun setAutoHideEnabled(autoHideEnabled: Boolean) {
        mScrollbar.setAutoHideEnabled(autoHideEnabled)
    }

    fun setOnFastScrollStateChangeListener(stateChangeListener: OnFastScrollStateChangeListener) {
        mStateChangeListener = stateChangeListener
    }

    @Deprecated("")
    fun setStateChangeListener(stateChangeListener: OnFastScrollStateChangeListener) {
        setOnFastScrollStateChangeListener(stateChangeListener)
    }

    fun setThumbInactiveColor(@ColorInt color: Int) {
        mScrollbar.setThumbInactiveColor(color)
    }

    fun allowThumbInactiveColor(allowInactiveColor: Boolean) {
        mScrollbar.enableThumbInactiveColor(allowInactiveColor)
    }

    @Deprecated("")
    fun setThumbInactiveColor(allowInactiveColor: Boolean) {
        allowThumbInactiveColor(allowInactiveColor)
    }

    fun setFastScrollEnabled(fastScrollEnabled: Boolean) {
        mFastScrollEnabled = fastScrollEnabled
    }

    @Deprecated("")
    fun setThumbEnabled(thumbEnabled: Boolean) {
        setFastScrollEnabled(thumbEnabled)
    }

    /**
     * Set the FastScroll Popup position. This is either [FastScroller.FastScrollerPopupPosition.ADJACENT],
     * meaning the popup moves adjacent to the FastScroll thumb, or [FastScroller.FastScrollerPopupPosition.CENTER],
     * meaning the popup is static and centered within the RecyclerView.
     */
    fun setPopupPosition(@FastScroller.FastScrollerPopupPosition popupPosition: Int) {
        mScrollbar.setPopupPosition(popupPosition)
    }

    private inner class ScrollOffsetInvalidator : RecyclerView.AdapterDataObserver() {
        private fun invalidateAllScrollOffsets() {
            mScrollOffsets.clear()
        }

        override fun onChanged() {
            invalidateAllScrollOffsets()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            invalidateAllScrollOffsets()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            invalidateAllScrollOffsets()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            invalidateAllScrollOffsets()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            invalidateAllScrollOffsets()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            invalidateAllScrollOffsets()
        }
    }

    interface SectionedAdapter {
        fun getSectionName(position: Int): String
    }

    /**
     * FastScrollRecyclerView by default assumes that all items in a RecyclerView will have
     * ItemViews with the same heights so that the total height of all views in the RecyclerView
     * can be calculated. If your list uses different view heights, then make your adapter implement
     * this interface.
     */
    interface MeasurableAdapter {
        /**
         * Gets the height of a specific view type, including item decorations
         * @param recyclerView The recyclerView that this item view will be placed in
         * @param viewHolder The viewHolder that corresponds to this item view
         * @param viewType The view type to get the height of
         * @return The height of a single view for the given view type in pixels
         */
        fun getViewHeight(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder?, postition: Int): Int
    }

    companion object {

        private val TAG = "FastScrollRecyclerView"
    }
}
