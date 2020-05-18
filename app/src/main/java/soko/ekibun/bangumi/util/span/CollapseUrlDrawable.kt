package soko.ekibun.bangumi.util.span

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.util.Size
import android.view.View
import android.widget.TextView
import android.widget.Toast
import soko.ekibun.bangumi.util.HtmlUtil

/**
 * 限制最大高度的 url drawable
 * @property gradientPaint Paint
 * @constructor
 */
open class CollapseUrlDrawable(
    wrapWidth: (Float) -> Float,
    sizeCache: HashMap<String, Size>
) : UrlDrawable(wrapWidth, sizeCache) {

    override var drawable: Drawable? = null
        set(drawable) {
            (field as? Animatable)?.stop()
            field?.callback = null
            field = drawable
            field?.callback = drawableCallback
            (drawable as? Animatable)?.start()
            val drawableBounds = drawable?.bounds ?: bounds
            setBounds(0, 0, drawableBounds.width(), Math.min(drawableBounds.height(), 250))
            mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
            updateBuffer()

            container?.get()?.let {
                val text = (it.text as? Spannable) ?: return@let
                text.getSpans(0, text.length, BaseLineImageSpan::class.java)?.filter { span ->
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

    override fun update(drawable: Drawable) {
        val size = {
            val width = wrapWidth(if (error == false) drawable.intrinsicWidth.toFloat() else -1f)
            Size(width.toInt(), (drawable.intrinsicHeight * width / drawable.intrinsicWidth).toInt())
        }()
        drawable.setBounds(0, 0, size.width, size.height)
        this.drawable = drawable
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

    /**
     * 限制最大高度url drawable ImageGetter
     */
    class CollapseImageGetter(container: TextView) : HtmlUtil.ImageGetter(wrapWidth = {
        Math.min(container.width.toFloat(), Math.max(container.textSize, it))
    }) {
        override val onClick: (View, BaseLineImageSpan) -> Unit = { itemView, span ->
            Toast.makeText(
                itemView.context,
                span.source ?: (span.drawable as? UrlDrawable)?.url, Toast.LENGTH_LONG
            ).show()
        }

        override fun createDrawable(): UrlDrawable {
            return CollapseUrlDrawable(wrapWidth, sizeCache)
        }
    }
}
