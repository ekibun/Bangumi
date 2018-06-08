package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.widget.TextView
import java.lang.IllegalArgumentException
import java.lang.reflect.Field

class StrokeTextView @JvmOverloads constructor(context: Context, private val outerColor: Int = 0, private val innerColor: Int = 0) : TextView(context) {

    private var textPaint: TextPaint = this.paint

    override fun onDraw(canvas: Canvas) {
        setTextColorUseReflection(outerColor)
        textPaint.strokeWidth = 3f // 描边宽度
        textPaint.style = Paint.Style.FILL_AND_STROKE // 描边种类
        textPaint.isFakeBoldText = true // 外层text采用粗体
        textPaint.setShadowLayer(0f, 0f, 0f, 0) // 字体的阴影效果，可以忽略
        super.onDraw(canvas)

        setTextColorUseReflection(innerColor)
        textPaint.strokeWidth = 0f
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.isFakeBoldText = false
        textPaint.setShadowLayer(0f, 0f, 0f, 0)
        super.onDraw(canvas)
    }

    private fun setTextColorUseReflection(color: Int) {
        val textColorField: Field
        try {
            textColorField = TextView::class.java.getDeclaredField("mCurTextColor")
            textColorField.isAccessible = true
            textColorField.set(this, color)
            textColorField.isAccessible = false
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        textPaint.color = color
    }

}