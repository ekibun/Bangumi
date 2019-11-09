package soko.ekibun.bangumi.util

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import java.lang.ref.WeakReference

class HtmlHttpImageGetter(container: TextView, private val drawables: ArrayList<String>, private val sizeInfos: HashMap<String, Size>) : Html.ImageGetter {
    private val container = WeakReference(container)

    init {
        (container.tag as? ArrayList<*>)?.mapNotNull { it as? Target<*> }?.forEach {
            GlideUtil.with(container)?.clear(it)
        }
        container.tag = ArrayList<Target<Drawable>>()
    }

    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable(container) {
            sizeInfos[source] = it
        }
        urlDrawable.url = Bangumi.parseUrl(source)
        urlDrawable.size = sizeInfos[source]
        drawables.add(source)
        urlDrawable.loadImage()
        sizeInfos[source]?.let {
            urlDrawable.setBounds(0, 0, it.width, it.height)
        }
        return urlDrawable
    }

    open class UrlDrawable(val container: WeakReference<TextView>, val updateSize: (Size) -> Unit = {}) : AnimationDrawable() {
        var drawable: Drawable? = null
        var error: Boolean? = null
        var size: Size? = null
        var url: String? = null
        var uri: Uri? = null
        val textSize get() = container.get()?.textSize ?: 10f
        val maxWidth get() = container.get()?.let { it.width.toFloat() - it.paddingLeft - it.paddingRight } ?: 1000f

        protected var mBuffer: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        private val mPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        private fun addTarget(target: Target<Drawable>) {
            container.get()?.let { v -> v.tag = (v.tag as? ArrayList<*>)?.toMutableList()?.add(target) }
        }

        open fun update(drawable: Drawable, defSize: Int) {
            val width = Math.max(textSize, Math.min(drawable.intrinsicWidth.toFloat(), maxWidth))

            val size = if (defSize > 0) this.size
                    ?: Size(defSize, defSize) else Size(width.toInt(), (drawable.intrinsicHeight * width / drawable.intrinsicWidth).toInt())
            this.size = size
            updateSize(size)
            (this.drawable as? Animatable)?.stop()
            this.drawable?.callback = null
            this.drawable = drawable
            this.drawable?.callback = drawableCallback
            (drawable as? Animatable)?.start()
            setBounds(0, 0, size.width, size.height)
            mBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
            this.drawable?.bounds = bounds
            updateBuffer()

            container.get()?.text = container.get()?.text
            container.get()?.invalidate()
        }

        open fun loadImage() {
            val url = this.url ?: return
            val view = container.get()
            view?.post {
                val textSize = view.textSize
                val circularProgressDrawable = CircularProgressDrawable(view.context)
                circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(view.context, android.R.attr.textColorSecondary))
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
                circularProgressDrawable.progressRotation = 0.75f
                circularProgressDrawable.start()
                GlideUtil.loadWithProgress(url, view, RequestOptions().placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image), false, uri) { type, drawable ->
                    error = when (type) {
                        GlideUtil.TYPE_RESOURCE -> false
                        GlideUtil.TYPE_ERROR -> true
                        else -> null
                    }
                    drawable?.let { update(it, if (type == GlideUtil.TYPE_RESOURCE) 0 else textSize.toInt()) }
                }?.let { addTarget(it) }
            }
        }

        open fun updateBuffer() {
            val bufferCanvas = Canvas(mBuffer)
            bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawable?.draw(bufferCanvas)
            invalidateSelf()
        }

        override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
            drawable?.setVisible(visible, restart)
            return super.setVisible(visible, restart)
        }

        val drawableCallback = object : Callback {
            override fun invalidateDrawable(who: Drawable) {
                updateBuffer()
                container.get()?.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                container.get()?.postDelayed(what, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                container.get()?.removeCallbacks(what)
            }
        }

        override fun draw(canvas: Canvas) {
            canvas.drawBitmap(mBuffer, bounds, bounds, mPaint)
        }
    }
}