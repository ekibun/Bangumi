package soko.ekibun.bangumi.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class RecyclerTabLayout constructor(context: Context, attrs: AttributeSet) :
    com.nshmura.recyclertablayout.RecyclerTabLayout(context, attrs) {
    override fun setUpWithAdapter(adapter: Adapter<*>) {
        mAdapter = adapter
        mViewPager = adapter.viewPager
        requireNotNull(mViewPager.adapter) { "ViewPager does not have a PagerAdapter set" }
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            var scrollState = ViewPager.SCROLL_STATE_DRAGGING
            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (scrollState == ViewPager.SCROLL_STATE_DRAGGING) {
                    scrollToTab(position, positionOffset, false)
                }
            }

            override fun onPageSelected(position: Int) {
                if (scrollState == ViewPager.SCROLL_STATE_SETTLING)
                    startAnimation(position)
            }

        })
        setAdapter(adapter)
        scrollToTab(mViewPager.currentItem)
    }

    override fun startAnimation(position: Int) {
        var distance = 0f
        val view = mLinearLayoutManager.findViewByPosition(position)
        if (view != null) {
            val currentX = view.x + view.measuredWidth / 2f
            val centerX = measuredWidth / 2f
            distance = (centerX - currentX) / view.measuredWidth
        }
        if (distance == 0f) return scrollToTab(position)
        val animator: ValueAnimator
        animator = ValueAnimator.ofFloat(distance, 0f)
        animator.duration = DEFAULT_SCROLL_DURATION
        animator.addUpdateListener { animation ->
            scrollToTab(position, animation.animatedValue as Float, true)
        }
        animator.start()
    }
}