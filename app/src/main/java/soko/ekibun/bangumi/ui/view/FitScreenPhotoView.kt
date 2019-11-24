@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView

/**
 * 适应屏幕PhotoView
 */
open class FitScreenPhotoView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    val isMinScale get() = scale == 1f

    /**
     * 更新Drawable
     */
    fun updateDrawable(drawable: Drawable?) {
        setImageDrawable(drawable)
        if (drawable == null) return
        if (drawable is Animatable) drawable.start()
        val w = drawable.intrinsicWidth * 1f
        val h = drawable.intrinsicHeight * 1f
        val W = measuredWidth * 1f
        val H = measuredHeight * 1f
        if (w <= 0 || h <= 0 || W <= 0 || H <= 0) return
        val minScale = Math.min(H / h, W / w)
        val maxScale = Math.max(H / h, W / w)
        val dif = maxScale / minScale
        when {
            dif > 3f -> setScaleLevels(1f, 3f, 5f)
            dif > 1f -> setScaleLevels(1f, dif, Math.max(1f + (dif - 1f) * 2, 3f))
            else -> setScaleLevels(1f, 1.75f, 3f)
        }
    }
}