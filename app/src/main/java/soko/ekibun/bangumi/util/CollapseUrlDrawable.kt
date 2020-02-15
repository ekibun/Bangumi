package soko.ekibun.bangumi.util

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.util.Size
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * 限制最大高度的 url drawable
 * @property gradientPaint Paint
 * @constructor
 */
open class CollapseUrlDrawable(container: WeakReference<TextView>) : UrlDrawable(container) {

    override fun update(drawable: Drawable, defSize: Int) {
        val width = Math.max(textSize, Math.min(drawable.intrinsicWidth.toFloat(), maxWidth))
        val size = if (defSize > 0) Size(defSize, defSize) else Size(width.toInt(), (drawable.intrinsicHeight * width / drawable.intrinsicWidth).toInt())
        (this.drawable as? Animatable)?.stop()
        this.drawable?.callback = null
        this.drawable = drawable
        this.drawable?.callback = drawableCallback
        (drawable as? Animatable)?.start()

        setBounds(0, 0, size.width, Math.min(size.height, 250))
        drawable.setBounds(0, 0, size.width, size.height)
        mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        this.drawable?.setBounds(0, 0, size.width, size.height)
        updateBuffer()

        container.get()?.let {
            it.editableText.getSpans(0, it.editableText.length, ImageSpan::class.java).filter { it.drawable == this }.forEach { span ->
                val start = it.editableText.getSpanStart(span)
                val end = it.editableText.getSpanEnd(span)
                val flags = it.editableText.getSpanFlags(span)

                it.editableText.removeSpan(span)
                it.editableText.setSpan(span, start, end, flags)
            }
            it.invalidate()
        }
    }

    private val gradientPaint by lazy {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = 0xFF000000.toInt()
        paint.shader = LinearGradient(0f, 200f, 0f, 250f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        paint
    }

    override fun updateBuffer() {
        val canvas = Canvas(mBuffer)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawable?.draw(canvas)
        if (bounds.height() != drawable?.bounds?.height()) {
            canvas.drawRect(bounds, gradientPaint)
        }
        invalidateSelf()
    }
}
