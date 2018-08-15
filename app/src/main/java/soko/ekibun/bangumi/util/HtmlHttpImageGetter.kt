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
import java.net.URI
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class HtmlHttpImageGetter(val container: TextView, private val baseUri: URI?, private val drawableMap: HashMap<String, HtmlHttpImageGetter.UrlDrawable>) : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        val urlDrawable  = drawableMap.getOrPut(source) {UrlDrawable()}
        if(urlDrawable.drawable == null) container.post {
            val url = baseUri?.resolve(source)?.toURL() ?: URI.create(source).toURL()
            // get the actual source
            Glide.with(container).asBitmap().load(url).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    val result = BitmapDrawable(container.resources, bitmap)
                    val maxWidth = container.width.toFloat()
                    val minWidth = container.textSize
                    val originalDrawableWidth = result.intrinsicWidth.toFloat()
                    val scale = min(maxWidth, max(minWidth, originalDrawableWidth)) / originalDrawableWidth
                    result.setBounds(0, 0, (result.intrinsicWidth * scale).toInt(), (result.intrinsicHeight * scale).toInt())
                    urlDrawable.drawable = result
                    urlDrawable.setBounds(0, 0, (result.intrinsicWidth * scale).toInt(), (result.intrinsicHeight * scale).toInt())
                    container.text = container.text
                    container.invalidate()
                }
                override fun onStart() {}
                override fun onDestroy() {}
            })
        }


        //val asyncTask = ImageGetterAsyncTask(urlDrawable, this, container)

        //asyncTask.execute(source)

        // return reference to URLDrawable which will asynchronously load the image specified in the src tag
        return urlDrawable
    }

    inner class UrlDrawable : BitmapDrawable() {
        var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }
    }
}