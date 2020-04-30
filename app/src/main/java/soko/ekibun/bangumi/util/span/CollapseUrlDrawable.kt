package soko.ekibun.bangumi.util.span

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.util.Log
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

    override fun update(drawable: Drawable) {
        val size = {
            val width = wrapWidth(if (error == false) drawable.intrinsicWidth.toFloat() else -1f)
            Size(width.toInt(), (drawable.intrinsicHeight * width / drawable.intrinsicWidth).toInt())
        }()
        Log.v("update", "$drawable size: $size")
        (this.drawable as? Animatable)?.stop()
        this.drawable?.callback = null
        this.drawable = drawable
        this.drawable?.callback = this.drawableCallback
        (drawable as? Animatable)?.start()
        setBounds(0, 0, size.width, Math.min(size.height, 250))
        mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        this.drawable?.setBounds(0, 0, size.width, size.height)
        updateBuffer()

        container?.get()?.let {
            it.editableText.getSpans(0, it.editableText.length, ClickableImageSpan::class.java).filter { span ->
                span.image.drawable == this
            }.forEach { span ->
                val start = it.editableText.getSpanStart(span)
                val end = it.editableText.getSpanEnd(span)
                val flags = it.editableText.getSpanFlags(span)

                it.editableText.removeSpan(span.image)
                span.image = ImageSpan(this, url ?: "", ImageSpan.ALIGN_BASELINE)
                it.editableText.setSpan(span.image, start, end, flags)
            }
            it.editableText.getSpans(0, it.editableText.length, ImageSpan::class.java).filter { span ->
                span.drawable == this
            }.forEach { span ->
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

    /**
     * 限制最大高度url drawable ImageGetter
     */
    class CollapseImageGetter(container: TextView) : HtmlUtil.ImageGetter(wrapWidth = {
        Math.min(container.width.toFloat(), Math.max(container.textSize, it))
    }) {
        override val onClick: (View, ImageSpan) -> Unit = { itemView, span ->
            Toast.makeText(itemView.context, span.source, Toast.LENGTH_LONG).show()
        }

        override fun createDrawable(): UrlDrawable {
            return CollapseUrlDrawable(wrapWidth, sizeCache)
        }
    }
}
