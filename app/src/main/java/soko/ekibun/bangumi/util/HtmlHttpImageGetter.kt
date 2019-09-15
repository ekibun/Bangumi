@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.util

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.request.RequestOptions
import pl.droidsonroids.gif.GifDrawable
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import java.lang.ref.WeakReference

class HtmlHttpImageGetter(container: TextView, private val drawables: ArrayList<String>, private val sizeInfos: HashMap<String, Size>) : Html.ImageGetter {
    private val container = WeakReference(container)
    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable(source, container, sizeInfos)
        drawables.add(source)
        urlDrawable.loadImage()
        sizeInfos[source]?.let {
            urlDrawable.setBounds(0, 0, it.width, it.height)
        }
        return urlDrawable
    }

    class UrlDrawable(val url: String, val container: WeakReference<TextView>, val sizeInfos: HashMap<String, Size>) : BitmapDrawable() {
        var drawable: Drawable? = null
        var error: Boolean? = null

        fun loadImage() {
            val view = container.get()
            view?.post {
                val update = { resource: Drawable, defSize: Int ->
                    val drawable = when (resource) {
                        is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                        else -> resource
                    }
                    val size = if (defSize > 0) sizeInfos[url]
                            ?: Size(defSize, defSize) else Size(resource.intrinsicWidth, resource.intrinsicHeight)
                    sizeInfos[url] = size
                    setBounds(0, 0, size.width, size.height)

                    drawable.setBounds(0, 0, size.width, size.height)
                    this.drawable?.callback = null
                    this.drawable = drawable
                    //}
                    container.get()?.text = container.get()?.text
                    container.get()?.invalidate()
                }
                val textSize = view.textSize
                val circularProgressDrawable = CircularProgressDrawable(view.context)
                circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(view.context, android.R.attr.textColorSecondary))
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
                circularProgressDrawable.progressRotation = 0.75f
                circularProgressDrawable.start()
                val url = Bangumi.parseUrl(url)
                GlideUtil.loadWithProgress(url, view,
                        RequestOptions().transform(SizeTransformation { width, _ ->
                            val maxWidth = container.get()?.width?.toFloat() ?: return@SizeTransformation 1f
                            val minWidth = container.get()?.textSize ?: return@SizeTransformation 1f
                            Math.min(maxWidth, Math.max(minWidth, width.toFloat())) / width
                        }).placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image),
                        viewTarget = false
                ) { type, drawable ->
                    error = when (type) {
                        GlideUtil.TYPE_RESOURCE -> false
                        GlideUtil.TYPE_ERROR -> true
                        else -> null
                    }
                    drawable?.let { update(it, if (type == GlideUtil.TYPE_RESOURCE) 0 else textSize.toInt()) }
                }
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