package soko.ekibun.bangumi.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import pl.droidsonroids.gif.GifDrawable
import java.net.URI
import android.util.Size
import com.bumptech.glide.request.RequestOptions
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class HtmlHttpImageGetter(container: TextView, private val baseUri: URI?, private val drawables: ArrayList<String>, private val sizeInfos: HashMap<String, Size>) : Html.ImageGetter {
    private val container = WeakReference(container)
    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable()
        drawables.add(source)
        if(urlDrawable.drawable == null)
            container.get()?.post {
                Glide.with(container.get()?:return@post)
                        .asDrawable().load(HttpUtil.getUrl(source, baseUri))
                        .apply(RequestOptions().transform(SizeTransformation {width, _ ->
                            val maxWidth = container.get()?.width?.toFloat()?:return@SizeTransformation 1f
                            val minWidth = container.get()?.textSize?:return@SizeTransformation 1f
                            Math.min(maxWidth, Math.max(minWidth, width.toFloat())) / width
                        }))
                        .into(object : SimpleTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                val drawable = when (resource) {
                                    is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                                    else -> resource
                                }
                                val size = Size(resource.intrinsicWidth, resource.intrinsicHeight)
                                sizeInfos[source] = size
                                urlDrawable.setBounds(0, 0, size.width, size.height)

                                drawable.setBounds(0, 0, size.width, size.height)
                                urlDrawable.drawable?.callback = null
                                urlDrawable.drawable = drawable
                                //}
                                container.get()?.text = container.get()?.text
                                container.get()?.invalidate()
                            }
                            override fun onStart() {}
                            override fun onDestroy() {}
                        })
            }
        sizeInfos[source]?.let{
            urlDrawable.setBounds(0, 0, it.width, it.height)
        }
        urlDrawable.container = container
        return urlDrawable
    }

    class UrlDrawable : BitmapDrawable() {
        var drawable: Drawable? = null
        var container: WeakReference<TextView>? = null

        override fun draw(canvas: Canvas) {
            (drawable as? GifDrawable)?.let{
                it.callback = object: Drawable.Callback{
                    override fun invalidateDrawable(who: Drawable) {
                        container?.get()?.invalidate()
                    }
                    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                        container?.get()?.postDelayed(what, `when`)
                    }
                    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                        container?.get()?.removeCallbacks(what)
                    }
                }
            }
            drawable?.draw(canvas)
        }
    }
}