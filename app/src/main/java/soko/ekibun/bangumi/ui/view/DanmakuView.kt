package soko.ekibun.bangumi.ui.view

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.support.v4.graphics.ColorUtils
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import soko.ekibun.bangumi.parser.Parser

class DanmakuView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {
    private var lastTime = 0
    private var rowLength = ArrayList<Int>()
    private val animations = ArrayList<Animator>()

    private val screenSize: Size by lazy {
        val p = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(p)
        Size(Math.min(p.x, p.y), Math.max(p.x, p.y))
    }

    fun add(danmakus: List<Parser.Danmaku>) {
        if(danmakus.isNotEmpty() && lastTime == danmakus[0].time)
            return
        for (d in danmakus) {
            this.post { createDanmakuView(d) }
        }
    }

    private var danmakuHeight: Int = 0

    private fun createDanmakuView(danmaku: Parser.Danmaku) {
        try {
            val clr = Color.parseColor(danmaku.color)
            val lur = ColorUtils.calculateLuminance(clr)
            val out = if( lur < 0.3) 255 else 0
            val textView = StrokeTextView(context, Color.argb(200, out, out, out), Color.parseColor(danmaku.color))
            textView.text = danmaku.context

            textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            danmakuHeight = textView.measuredHeight + 5

            if (lastTime > danmaku.time) {
                rowLength = ArrayList()
            }
            lastTime = danmaku.time
            var row = -1
            for (i in rowLength.indices) {
                if (danmaku.time * 200 > rowLength[i]) {
                    row = i
                    rowLength[i] = danmaku.time * 200 + textView.measuredWidth + 20
                    break
                }
            }
            if (row < 0) {
                row = rowLength.size
                rowLength.add(danmaku.time * 200 + textView.measuredWidth + 20)
            }
            val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = row * danmakuHeight
            if ((row + 1) * danmakuHeight > this.height)
                return
            textView.layoutParams = lp
            this.addView(textView)

            val s = screenSize.width * 1.5 + textView.measuredWidth
            val animator = textView.animate()
                    .translationXBy((-s-textView.measuredWidth).toFloat())
            animator.duration = (5 * s).toLong()
            animator.interpolator = LinearInterpolator()
            animator.setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(ani: Animator) {
                    animations.add(ani)
                }

                override fun onAnimationEnd(ani: Animator) {
                    this@DanmakuView.removeView(textView)
                    animations.remove(ani)
                }

                override fun onAnimationCancel(ani: Animator) {}
                override fun onAnimationRepeat(ani: Animator) {}
            })
            animator.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause(){
        for(ani in animations){
            ani.pause()
        }
    }

    fun resume(){
        for(ani in animations){
            ani.resume()
        }
    }

    fun clear(){
        this.removeAllViews()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val scale = if(r-l > screenSize.width){ 1.5f } else{ 1f }
        scaleX = scale
        scaleY = scale
        pivotX = (r - l).toFloat()
        pivotY = 0f

        super.onLayout(changed, l, t, r, b)
        val childCount = this.childCount
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val lp = view.layoutParams as RelativeLayout.LayoutParams
            if (lp.leftMargin <= 0) {
                view.layout(r - l, lp.topMargin, r - l + (1.2 * view.measuredWidth).toInt(),
                        lp.topMargin + view.measuredHeight)

            } else {
                continue
            }
        }
    }
}