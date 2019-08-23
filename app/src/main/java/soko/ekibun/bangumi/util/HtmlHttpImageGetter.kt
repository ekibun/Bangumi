@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.util

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import pl.droidsonroids.gif.GifDrawable
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import java.lang.ref.WeakReference
import java.net.URI

class HtmlHttpImageGetter(container: TextView, private val baseUri: URI?, private val drawables: ArrayList<String>, private val sizeInfos: HashMap<String, Size>) : Html.ImageGetter {
    private val container = WeakReference(container)
    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable(source, baseUri, container,sizeInfos)
        drawables.add(source)
        urlDrawable.loadImage()
        sizeInfos[source]?.let{
            urlDrawable.setBounds(0, 0, it.width, it.height)
        }
        return urlDrawable
    }

    class UrlDrawable(val url: String, val baseUri: URI?, val container: WeakReference<TextView>, val sizeInfos: HashMap<String, Size>) : BitmapDrawable() {
        var drawable: Drawable? = null
        var error: Boolean? = null

        fun loadImage(){
            val view = container.get()
            view?.post {
                val update = {resource: Drawable, defSize: Int ->
                    val drawable = when (resource) {
                        is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                        else -> resource
                    }
                    val size =  if(defSize > 0) sizeInfos[url]?: Size(defSize, defSize) else Size(resource.intrinsicWidth, resource.intrinsicHeight)
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
                val url = HttpUtil.getUrl(url, baseUri)
                ProgressAppGlideModule.expect(url, object : ProgressAppGlideModule.UIonProgressListener {
                    override fun onProgress(bytesRead: Long, expectedLength: Long) {
                        if(circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                        circularProgressDrawable.setStartEndTrim(0f, bytesRead * 1f / expectedLength)
                        circularProgressDrawable.progressRotation = 0.75f
                        circularProgressDrawable.invalidateSelf()
                    }

                    override fun getGranualityPercentage(): Float {
                        return 1.0f
                    }
                })
                GlideUtil.with(view)
                        ?.asDrawable()?.load(GlideUrl(url, Headers {
                            mapOf("referer" to url,
                                    "user-agent" to App.getUserAgent(view.context)
                            )
                        }))
                        ?.apply(RequestOptions().transform(SizeTransformation { width, _ ->
                            val maxWidth = container.get()?.width?.toFloat()?:return@SizeTransformation 1f
                            val minWidth = container.get()?.textSize?:return@SizeTransformation 1f
                            Math.min(maxWidth, Math.max(minWidth, width.toFloat())) / width
                        }).placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image))
                        ?.into(object : SimpleTarget<Drawable>() {
                            override fun onLoadStarted(placeholder: Drawable?) {
                                error = null
                                placeholder?.let{ update(it, textSize.toInt()) }
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                error = true
                                if(circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                                errorDrawable?.let{ update(it, textSize.toInt()) }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                error = null
                                placeholder?.let{ update(it, textSize.toInt()) }
                            }

                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                if(circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                                error = false
                                update(resource, 0)
                            }

                            override fun onStart() {}
                            override fun onDestroy() {
                                ProgressAppGlideModule.forget(url)
                            }
                        })
            }
        }

        override fun draw(canvas: Canvas) {
            drawable?.callback = object: Callback {
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