package soko.ekibun.bangumi.util.span

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * url图片Drawable
 * @constructor
 */
open class UrlDrawable(
    val wrapWidth: (Float) -> Float,
    val sizeCache: HashMap<String, Size>
) : TextViewDrawable() {

    private var target: Target<Drawable>? = null
    fun cancel() {
        container?.get()?.let { GlideUtil.with(it) }?.clear(target)
        error = null
        drawable = null
    }

    /**
     * 加载状态
     * - true: 加载失败
     * - false：加载成功
     * - null：加载中
     */
    var error: Boolean? = null
    var url: String? = null
    var uri: Uri? = null

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
        drawable.setBounds(0, 0, size.width, size.height)
        this.drawable = drawable
    }

    /**
     * 加载图片
     */
    open fun loadImage() {
        if (error == false) return // 加载完成不再加载
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
                Log.v("Type", type.toString())
                error = when (type) {
                    GlideUtil.TYPE_RESOURCE -> false
                    GlideUtil.TYPE_ERROR -> true
                    else -> null
                }
                drawable?.let { update(it) }
            }
        }
    }
}