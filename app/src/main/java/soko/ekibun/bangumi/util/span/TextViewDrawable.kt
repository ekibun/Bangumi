package soko.ekibun.bangumi.util.span

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ImageSpan
import android.widget.TextView
import java.lang.ref.WeakReference

open class TextViewDrawable : AnimationDrawable() {
    var container: WeakReference<TextView>? = null

    open var drawable: Drawable? = null
        set(drawable) {
            (field as? Animatable)?.stop()
            field?.callback = null
            field = drawable
            if (drawable == null) return
            drawable.callback = drawableCallback
            (drawable as? Animatable)?.start()
            bounds = drawable.bounds
            mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
            updateBuffer()

            container?.get()?.let {
                val text = (it.text as? Spannable) ?: return@let
                text.getSpans(0, text.length, ImageSpan::class.java)?.filter { span ->
                    span.drawable == this
                }?.forEach { span ->
                    val start = text.getSpanStart(span)
                    val end = text.getSpanEnd(span)
                    val flags = text.getSpanFlags(span)
                    text.removeSpan(span)
                    text.setSpan(span, start, end, flags)
                }
                it.invalidate()
            }
        }

    internal var mBuffer: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val mPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    open fun updateBuffer() {
        val bufferCanvas = Canvas(mBuffer)
        bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawable?.draw(bufferCanvas)
        invalidateSelf()
    }

    internal val drawableCallback = object : Callback {
        override fun invalidateDrawable(who: Drawable) {
            updateBuffer()
            container?.get()?.invalidate()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            container?.get()?.postDelayed(what, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            container?.get()?.removeCallbacks(what)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(mBuffer, bounds, bounds, mPaint)
    }
}