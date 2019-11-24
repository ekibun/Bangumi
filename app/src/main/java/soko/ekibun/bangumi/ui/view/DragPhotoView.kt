package soko.ekibun.bangumi.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.ResourceUtil
import java.util.*
import kotlin.math.abs
import kotlin.math.max

/**
 * 下拉关闭
 * 带加载进度条
 */
class DragPhotoView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : FitScreenPhotoView(context, attr, defStyle) {
    val circularProgressDrawable by lazy {
        val circularProgressDrawable = CircularProgressDrawable(context)
        circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(context, R.attr.colorAccent))
        circularProgressDrawable.strokeWidth = 8f
        circularProgressDrawable.centerRadius = 50 - circularProgressDrawable.strokeWidth
        circularProgressDrawable.progressRotation = 0.75f
        circularProgressDrawable.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                postDelayed(what, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                removeCallbacks(what)
            }
        }
        circularProgressDrawable
    }


    private val mPaint: Paint = Paint()

    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()

    private var mTranslateY: Float = 0.toFloat()
    private var mTranslateX: Float = 0.toFloat()
    private var mScale = 1f
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var minDragDownScale = 0.5f
    private var mAlpha = 255
    private var isAnimate = false

    //is event on PhotoView
    private var isTouchEvent = false
    var mTapListener: (() -> Unit)? = null
    var mExitListener: (() -> Unit)? = null
    var mLongClickListener: (() -> Unit)? = null

    private val alphaAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofInt(mAlpha, 255)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mAlpha = valueAnimator.animatedValue as Int }
            return animator
        }

    private val translateYAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mTranslateY, 0f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mTranslateY = valueAnimator.animatedValue as Float }
            return animator
        }

    private val translateXAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mTranslateX, 0f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mTranslateX = valueAnimator.animatedValue as Float }
            return animator
        }

    private val scaleAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mScale, 1f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator ->
                mScale = valueAnimator.animatedValue as Float
                invalidate()
            }

            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    isAnimate = true
                }

                override fun onAnimationEnd(animator: Animator) {
                    isAnimate = false
                    animator.removeAllListeners()
                }

                override fun onAnimationCancel(animator: Animator) { /* no-op */
                }

                override fun onAnimationRepeat(animator: Animator) { /* no-op */
                }
            })
            return animator
        }

    init {
        mPaint.color = Color.BLACK
    }

    //override fun dispatchDraw(canvas: Canvas) {
    override fun onDraw(canvas: Canvas) {
        mPaint.alpha = mAlpha
        canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mPaint)
        canvas.save()
        canvas.translate(mTranslateX, mTranslateY)
        canvas.scale(mScale, mScale, mWidth.toFloat() / 2, mHeight.toFloat() / 2)
        super.onDraw(canvas)
        canvas.restore()
        if (circularProgressDrawable.isVisible) circularProgressDrawable.draw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        circularProgressDrawable.bounds = Rect(0, 0, w, h)
        mWidth = w
        mHeight = h
    }

    private var timer = Timer()
    private var timeoutTask: TimerTask? = null
    private var longClick = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        val moveY = event.y
        val moveX = event.x
        val translateX = moveX - mDownX
        val translateY = moveY - mDownY

        if (isMinScale) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event)
                    longClick = false
                    timeoutTask?.cancel()
                    timeoutTask = object : TimerTask() {
                        override fun run() {
                            if (!isMinScale) return
                            this@DragPhotoView.post { mLongClickListener?.invoke() }
                            longClick = true
                        }
                    }
                    timer.schedule(timeoutTask, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (translateY != 0f || translateX != 0f) {
                        timeoutTask?.cancel()
                    }
                    //in viewpager
                    //如果不消费事件，则不作操作
                    if (!isTouchEvent && abs(translateY) < abs(translateX)) {
                        mScale = 1f
                        performAnimation()
                        return super.dispatchTouchEvent(event)
                    }

                    //single finger drag  down
                    //如果有上下位移 则不交给viewpager
                    if (event.pointerCount == 1) {
                        if (isTouchEvent)
                            onActionMove(event)

                        if (abs(translateY) > abs(translateX)) {
                            isTouchEvent = true
                        }
                        return true
                    }

                    //防止下拉的时候双手缩放
                    if (isTouchEvent) {
                        return true
                    }
                }

                MotionEvent.ACTION_UP ->
                    //防止下拉的时候双手缩放
                    if (event.pointerCount == 1) {
                        timeoutTask?.cancel()
                        if (translateX == 0f && translateY == 0f && !longClick) {
                            timeoutTask = object : TimerTask() {
                                override fun run() {
                                    if (!isMinScale) return
                                    this@DragPhotoView.post { mTapListener?.invoke() }
                                }
                            }
                            timer.schedule(timeoutTask, ViewConfiguration.getDoubleTapTimeout().toLong())
                        }

                        if (mTranslateY > MAX_TRANSLATE_Y) {
                            mExitListener?.invoke()
                        } else {
                            performAnimation()
                        }
                        isTouchEvent = false
                    }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun onActionMove(event: MotionEvent) {
        val moveY = event.y
        val moveX = event.x
        mTranslateX = moveX - mDownX
        mTranslateY = moveY - mDownY

        val percent = max(0f, mTranslateY) / MAX_TRANSLATE_Y

        if (mScale in minDragDownScale..1f) {
            mScale = (1 - percent) * 0.5f + 0.5f

            mAlpha = (155 * (1 - percent)).toInt() + 100
            if (mAlpha > 255) {
                mAlpha = 255
            } else if (mAlpha < 100) {
                mAlpha = 100
            }
        }
        if (mScale < minDragDownScale) {
            mScale = minDragDownScale
        } else if (mScale > 1f) {
            mScale = 1f
        }
        if (mTranslateY > 0) {
            mTranslateX += (mDownX - mWidth / 2) * (1 - mScale)
            mTranslateY += (mDownY - mHeight / 2) * (1 - mScale)
        }

        invalidate()
    }

    private fun performAnimation() {
        scaleAnimation.start()
        translateXAnimation.start()
        translateYAnimation.start()
        alphaAnimation.start()
    }

    private fun onActionDown(event: MotionEvent) {
        mDownX = event.x
        mDownY = event.y
    }

    companion object {
        private const val MAX_TRANSLATE_Y = 500
        private const val DURATION: Long = 300
    }
}