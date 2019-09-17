@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.util

import android.graphics.Canvas
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import pl.droidsonroids.gif.GifDrawable
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

        private fun addTarget(target: Target<Drawable>) {
            container.get()?.let { v -> v.tag = (v.tag as? ArrayList<*>)?.toMutableList()?.add(target) }
        }

        open fun update(resource: Drawable, defSize: Int) {
            val drawable = when (resource) {
                is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                else -> resource
            }
            val size = if (defSize > 0) this.size
                    ?: Size(defSize, defSize) else Size(resource.intrinsicWidth, resource.intrinsicHeight)
            this.size = size
            updateSize(size)
            setBounds(0, 0, size.width, size.height)

            drawable.setBounds(0, 0, size.width, size.height)
            this.drawable?.callback = null
            this.drawable = drawable
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
                GlideUtil.loadWithProgress(url, view, RequestOptions().transform(SizeTransformation { width, _ ->
                    val maxWidth = view.width.toFloat() - view.paddingLeft - view.paddingRight
                    Math.min(maxWidth, Math.max(textSize, width.toFloat())) / width
                }).placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image), false, uri) { type, drawable ->
                    error = when (type) {
                        GlideUtil.TYPE_RESOURCE -> false
                        GlideUtil.TYPE_ERROR -> true
                        else -> null
                    }
                    drawable?.let { update(it, if (type == GlideUtil.TYPE_RESOURCE) 0 else textSize.toInt()) }
                }?.let { addTarget(it) }
            }
        }

        override fun draw(canvas: Canvas) {
            drawable?.callback = object : Callback {
                override fun invalidateDrawable(who: Drawable) {
                    if (who is CircularProgressDrawable) container.get()?.let { it.text = it.text }
                    container.get()?.invalidate()
                }

                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                    container.get()?.postDelayed(what, `when`)
                }

                override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                    container.get()?.removeCallbacks(what)
                }
            }
            drawable?.draw(canvas)
        }
    }
}