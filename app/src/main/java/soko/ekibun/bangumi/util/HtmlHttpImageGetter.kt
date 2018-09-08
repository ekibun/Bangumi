package soko.ekibun.bangumi.util

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
import kotlin.math.max
import kotlin.math.min
import android.graphics.BitmapFactory
import android.graphics.Bitmap.CompressFormat
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


@Suppress("DEPRECATION")
class HtmlHttpImageGetter(container: TextView, private val baseUri: URI?, private val drawableMap: HashMap<String, HtmlHttpImageGetter.UrlDrawable>) : Html.ImageGetter {
    private val widget = WeakReference(container)
    override fun getDrawable(source: String): Drawable {
        val urlDrawable  = drawableMap.getOrPut(source) {UrlDrawable()}
        val container = widget.get()?:return urlDrawable
        if(urlDrawable.drawable == null) container.post {
            Glide.with(container).asDrawable().load(HttpUtil.getUrl(source, baseUri))
                    .into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val maxWidth = container.width.toFloat()
                    val minWidth = container.textSize
                    val originalDrawableWidth = resource.intrinsicWidth.toFloat()
                    val scale = min(maxWidth, max(minWidth, originalDrawableWidth)) / originalDrawableWidth

                    val drawable = when (resource) {
                        is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                        is BitmapDrawable -> {
                            val baos = ByteArrayOutputStream()
                            resource.bitmap.compress(CompressFormat.PNG, Math.min((scale * 90).toInt(), 90), baos)
                            val bytes = baos.toByteArray()
                            BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        }
                        else -> resource
                    }

                    drawable.setBounds(0, 0, (resource.intrinsicWidth * scale).toInt(), (resource.intrinsicHeight * scale).toInt())
                    urlDrawable.drawable = drawable
                    urlDrawable.setBounds(0, 0, (resource.intrinsicWidth * scale).toInt(), (resource.intrinsicHeight * scale).toInt())
                    container.text = container.text
                    container.invalidate()
                }
                override fun onStart() {}
                override fun onDestroy() {}
            })
        }
        urlDrawable.container = container
        return urlDrawable
    }

    class UrlDrawable : BitmapDrawable() {
        var drawable: Drawable? = null
        var container: TextView? = null

        override fun draw(canvas: Canvas) {
            (drawable as? GifDrawable)?.let{
                container?.unscheduleDrawable(it)
                it.callback = object: Drawable.Callback{
                    override fun invalidateDrawable(who: Drawable) {
                        container?.invalidate()
                    }
                    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                        container?.postDelayed(what, `when`)
                    }
                    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                        container?.removeCallbacks(what)
                    }
                }
            }
            drawable?.draw(canvas)
        }
    }
}