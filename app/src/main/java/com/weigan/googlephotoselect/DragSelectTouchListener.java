package com.weigan.googlephotoselect;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2016/5/7.
 */
public class DragSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private boolean isActive;
    private int start, end;

    private onSelectListener selectListener;

    private RecyclerView recyclerView;

    private static final int DELAY = 25;

    private int autoScrollDistance = (int) (Resources.getSystem().getDisplayMetrics().density * 56);

    private int mTopBound, mBottomBound;

    private boolean inTopSpot, inBottomSpot;

    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());

    private int scrollDistance;

    private float lastX, lastY;

    private static final int MAX_SCROLL_DISTANCE = 16;

    //这个数越大，滚动的速度增加越慢
    private static final int SCROLL_FECTOR = 6;

    private int lastStart, lastEnd;

    private ScrollerCompat scroller;

    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!inTopSpot && !inBottomSpot) {
                return;
            }
            scrollBy(scrollDistance);
            autoScrollHandler.postDelayed(this, DELAY);
        }
    };

    public void setSelectListener(onSelectListener selectListener) {
        this.selectListener = selectListener;
    }

    public interface onSelectListener{
        /**
         * 选择结果的回调
         * @param start 开始的位置
         * @param end 结束的位置
         * @param isSelected 是否选中
         */
        void onSelectChange(int start, int end, boolean isSelected);
    };

    public DragSelectTouchListener() {
        reset();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (!isActive || rv.getAdapter().getItemCount() == 0) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(e);
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d("weigan", "onInterceptTouchEvent ACTION_POINTER_DOWN");
            case MotionEvent.ACTION_DOWN:
                Log.d("weigan", "onInterceptTouchEvent ACTION_DOWN");
                reset();
                break;
        }
        recyclerView = rv;
        int height = rv.getHeight();
        mTopBound = -20;
        mBottomBound = height - autoScrollDistance;
        return true;
    }

    public void startAutoScroll() {
        if (recyclerView == null) {
            return;
        }
        initScroller(recyclerView.getContext());
        if (scroller.isFinished()) {
            recyclerView.removeCallbacks(scrollRun);
            scroller.startScroll(0, scroller.getCurrY(), 0, 5000, 100000);
            ViewCompat.postOnAnimation(recyclerView, scrollRun);
        }
    }

    private void initScroller(Context context) {
        if (scroller == null) {
            scroller = ScrollerCompat.create(context, new LinearInterpolator());
        }
    }

    public void stopAutoScroll() {
        if (scroller != null && !scroller.isFinished()) {
            recyclerView.removeCallbacks(scrollRun);
            scroller.abortAnimation();
        }
    }

    private Runnable scrollRun = new Runnable() {
        @Override
        public void run() {
            if (scroller != null && scroller.computeScrollOffset()) {
                Log.d("weigan", "scrollRun called");
                scrollBy(scrollDistance);
                ViewCompat.postOnAnimation(recyclerView, scrollRun);
            }
        }
    };

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        if (!isActive) {
            return;
        }
        int action = MotionEventCompat.getActionMasked(e);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (!inTopSpot && !inBottomSpot) {
                    //更新滑动选择区域
                    updateSelectedRange(rv, e);
                }
                //在顶部或者底部触发自动滑动
                processAutoScroll(e);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                //结束滑动选择，初始化各状态值
                reset();
                break;
        }
    }

    private void updateSelectedRange(RecyclerView rv, MotionEvent e) {
        updateSelectedRange(rv, e.getX(), e.getY());
    }

    private void updateSelectedRange(RecyclerView rv, float x, float y) {
        View child = rv.findChildViewUnder(x, y);
        if (child != null) {
            int position = rv.getChildAdapterPosition(child);
            if (position != RecyclerView.NO_POSITION && end != position) {
                end = position;
                notifySelectRangeChange();
            }
        }
    }


    private void processAutoScroll(MotionEvent event) {
        int y = (int) event.getY();
        if (y < mTopBound) {
            lastX = event.getX();
            lastY = event.getY();
            scrollDistance = -(mTopBound - y) / SCROLL_FECTOR;
            if (!inTopSpot) {
                inTopSpot = true;
//                autoScrollHandler.removeCallbacks(scrollRunnable);
//                autoScrollHandler.postDelayed(scrollRunnable, DELAY);
                startAutoScroll();
            }
        }else if (y > mBottomBound) {
            lastX = event.getX();
            lastY = event.getY();
            scrollDistance = (y - mBottomBound) / SCROLL_FECTOR;
            if (!inBottomSpot) {
                inBottomSpot = true;
//                autoScrollHandler.removeCallbacks(scrollRunnable);
//                autoScrollHandler.postDelayed(scrollRunnable, DELAY);
                startAutoScroll();
            }
        } else {
//            autoScrollHandler.removeCallbacks(scrollRunnable);
            inBottomSpot = false;
            inTopSpot = false;
            lastX = Float.MIN_VALUE;
            lastY = Float.MIN_VALUE;
            stopAutoScroll();
        }
    }

    private void notifySelectRangeChange() {
        if (selectListener == null) {
            return;
        }
        if (start == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION) {
            return;
        }

        int newStart, newEnd;
        newStart = Math.min(start, end);
        newEnd = Math.max(start, end);
        if (lastStart == RecyclerView.NO_POSITION || lastEnd == RecyclerView.NO_POSITION) {
            if (newEnd - newStart == 1) {
                selectListener.onSelectChange(newStart, newStart, true);
            } else {
                selectListener.onSelectChange(newStart, newEnd, true);
            }
        } else {
            if (newStart > lastStart) {
                selectListener.onSelectChange(lastStart, newStart - 1, false);
            } else if (newStart < lastStart) {
                selectListener.onSelectChange(newStart, lastStart - 1, true);
            }

            if (newEnd > lastEnd) {
                selectListener.onSelectChange(lastEnd + 1, newEnd, true);
            } else if (newEnd < lastEnd) {
                selectListener.onSelectChange(newEnd + 1, lastEnd, false);
            }
        }

        lastStart = newStart;
        lastEnd = newEnd;
    }

    private void reset() {
        setIsActive(false);
        start = RecyclerView.NO_POSITION;
        end = RecyclerView.NO_POSITION;
        lastStart = RecyclerView.NO_POSITION;
        lastEnd = RecyclerView.NO_POSITION;
        autoScrollHandler.removeCallbacks(scrollRunnable);
        inTopSpot = false;
        inBottomSpot = false;
        lastX = Float.MIN_VALUE;
        lastY = Float.MIN_VALUE;
        stopAutoScroll();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private void scrollBy(int distance) {
        int scrollDistance;
        if (distance > 0) {
            scrollDistance = Math.min(distance, MAX_SCROLL_DISTANCE);
        } else {
            scrollDistance = Math.max(distance, -MAX_SCROLL_DISTANCE);
        }
        recyclerView.scrollBy(0, scrollDistance);
        if (lastX != Float.MIN_VALUE && lastY != Float.MIN_VALUE) {
            updateSelectedRange(recyclerView, lastX, lastY);
        }
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setStartSelectPosition(int position) {
        setIsActive(true);
        start = position;
        end = position;
        lastStart = position;
        lastEnd = position;
    }
}