package soko.ekibun.bangumi.util.span

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Size
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil
import java.lang.ref.WeakReference

/**
 * url图片Drawable
 * @constructor
 */
open class UrlDrawable(
    val wrapWidth: (Float) -> Float,
    val sizeCache: HashMap<String, Size>
) : AnimationDrawable() {
    var container: WeakReference<TextView>? = null

    private var target: Target<Drawable>? = null
    fun cancel() {
        container?.get()?.let { GlideUtil.with(it) }?.clear(target)
    }

    internal var drawable: Drawable? = null

    /**
     * 加载状态
     * - true: 加载失败
     * - false：加载成功
     * - null：加载中
     */
    var error: Boolean? = null
    var url: String? = null
    var uri: Uri? = null

    internal var mBuffer: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    internal val mPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    /**
     * 更新Drawable
     */
    open fun update(drawable: Drawable) {
        val size = sizeCache[url] ?: {
            val width = wrapWidth(if (error == false) drawable.intrinsicWidth.toFloat() else -1f)
            val size = Size(width.toInt(), (drawable.intrinsicHeight * width / drawable.intrinsicWidth).toInt())
            if (error == false) url?.let { sizeCache[it] = size }
            size
        }()
        (this.drawable as? Animatable)?.stop()
        this.drawable?.callback = null
        this.drawable = drawable
        this.drawable?.callback = drawableCallback
        (drawable as? Animatable)?.start()
        setBounds(0, 0, size.width, size.height)
        mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        this.drawable?.bounds = bounds
        updateBuffer()

        container?.get()?.text = container?.get()?.text
        container?.get()?.invalidate()
    }

    /**
     * 加载图片
     */
    open fun loadImage() {
        val url = this.url ?: return
        val view = container?.get()
        view?.post {
            val textSize = view.textSize
            val circularProgressDrawable = CircularProgressDrawable(view.context)
            circularProgressDrawable.setColorSchemeColors(
                ResourceUtil.resolveColorAttr(
                    view.context,
                    android.R.attr.textColorSecondary
                )
            )
            circularProgressDrawable.strokeWidth = textSize / 8f
            circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
            circularProgressDrawable.progressRotation = 0.75f
            target = GlideUtil.loadWithProgress(
                url,
                view,
                RequestOptions().placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image),
                circularProgressDrawable,
                uri
            ) { type, drawable ->
                error = when (type) {
                    GlideUtil.TYPE_RESOURCE -> false
                    GlideUtil.TYPE_ERROR -> true
                    else -> null
                }
                drawable?.let { update(it) }
            }
        }
    }

    /**
     * 更新缓存
     */
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