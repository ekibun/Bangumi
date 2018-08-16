package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.animation.Animator
import android.view.MotionEvent
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.chrisbanes.photoview.PhotoView


class DragPhotoView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    private val mPaint: Paint = Paint()

    // downX
    private var mDownX: Float = 0.toFloat()
    // down Y
    private var mDownY: Float = 0.toFloat()

    private var mTranslateY: Float = 0.toFloat()
    private var mTranslateX: Float = 0.toFloat()
    private var mScale = 1f
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    var minScale = 0.5f
    private var mAlpha = 255
    private var canFinish = false
    private var isAnimate = false

    //is event on PhotoView
    private var isTouchEvent = false
    var mTapListener: (()->Unit)? = null
    var mExitListener: (()->Unit)? = null

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
                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
            return animator
        }

    init {
        mPaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        mPaint.alpha = mAlpha
        canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mPaint)
        canvas.translate(mTranslateX, mTranslateY)
        canvas.scale(mScale, mScale, mWidth.toFloat() / 2, mHeight.toFloat() / 2)
        super.onDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = w
        mHeight = h
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        //only scale == 1 can drag
        if (scale == 1f) {

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event)
                    //change the canFinish flag
                    canFinish = !canFinish
                }
                MotionEvent.ACTION_MOVE -> {
                    //in viewpager
                    if (mTranslateY == 0f && mTranslateX != 0f) {

                        //如果不消费事件，则不作操作
                        if (!isTouchEvent) {
                            mScale = 1f
                            performAnimation()
                            return super.dispatchTouchEvent(event)
                        }
                    }

                    //single finger drag  down
                    if (mTranslateY >= 0 && event.pointerCount == 1) {
                        onActionMove(event)

                        //如果有上下位移 则不交给viewpager
                        if (mTranslateY > 100) {
                            isTouchEvent = true
                        }
                        return true
                    }


                    //防止下拉的时候双手缩放
                    if (mTranslateY >= 0 && mScale < 0.95) {
                        return true
                    }
                }

                MotionEvent.ACTION_UP ->
                    //防止下拉的时候双手缩放
                    if (event.pointerCount == 1) {
                        onActionUp(event)
                        isTouchEvent = false
                        //judge finish or not
                        postDelayed({
                            if (mTranslateX == 0f && mTranslateY == 0f && canFinish) {
                                mTapListener?.invoke()
                            }
                            canFinish = false
                        }, 300)
                    }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun onActionUp(event: MotionEvent) {

        if (mTranslateY > MAX_TRANSLATE_Y) {
            mExitListener?.invoke()//!!.onExit(this, mTranslateX, mTranslateY, mWidth.toFloat(), mHeight.toFloat())
        } else {
            performAnimation()
        }
    }

    private fun onActionMove(event: MotionEvent) {
        val moveY = event.y
        val moveX = event.x
        mTranslateX = moveX - mDownX
        mTranslateY = moveY - mDownY

        //保证上划到到顶还可以继续滑动
        if (mTranslateY < 0) {
            mTranslateY = 0f
        }

        val percent = mTranslateY / MAX_TRANSLATE_Y
        if (mScale in minScale..1f) {
            mScale = (1 - percent)* 0.5f + 0.5f

            mAlpha = (155 * (1 - percent)).toInt() + 100
            if (mAlpha > 255) {
                mAlpha = 255
            } else if (mAlpha < 100) {
                mAlpha = 100
            }
        }
        if (mScale < minScale) {
            mScale = minScale
        } else if (mScale > 1f) {
            mScale = 1f
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

    fun finishAnimationCallBack() {
        mTranslateX = -mWidth / 2 + mWidth * mScale / 2
        mTranslateY = -mHeight / 2 + mHeight * mScale / 2
        invalidate()
    }

    companion object {
        private val MAX_TRANSLATE_Y = 500

        private val DURATION: Long = 300
    }
}