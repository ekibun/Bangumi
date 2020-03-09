package soko.ekibun.bangumi.util

import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Size
import android.widget.TextView
import com.bumptech.glide.request.target.Target
import soko.ekibun.bangumi.api.bangumi.Bangumi
import java.lang.ref.WeakReference

/**
 * 自定义http图片解析
 * @property drawables ArrayList<String>
 * @property sizeInfos HashMap<String, Size>
 * @property container WeakReference<(android.widget.TextView..android.widget.TextView?)>
 * @constructor
 */
class HtmlHttpImageGetter(
    container: TextView,
    private val drawables: ArrayList<String>,
    private val sizeInfos: HashMap<String, Size>,
    val maxWidth: () -> Float
) : Html.ImageGetter {
    private val container = WeakReference(container)

    init {
        (container.tag as? ArrayList<*>)?.mapNotNull { it as? Target<*> }?.forEach {
            GlideUtil.with(container)?.clear(it)
        }
        container.tag = ArrayList<Target<Drawable>>()
    }

    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable(container, maxWidth) {
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
}