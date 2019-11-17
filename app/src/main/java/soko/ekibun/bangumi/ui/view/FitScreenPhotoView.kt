@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView

/**
 * 适应屏幕PhotoView
 */
open class FitScreenPhotoView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    val isMinScale get() = scale == 1f

    val glideTarget = MyViewTarget(this)

    /**
     * 自定义ViewTarget
     */
    class MyViewTarget(private val view: FitScreenPhotoView) : CustomTarget<Drawable>(), Transition.ViewAdapter {
        override fun getView(): View {
            return view
        }

        private var animatable: Animatable? = null
        override fun getCurrentDrawable(): Drawable? {
            return view.drawable
        }

        override fun setDrawable(drawable: Drawable?) {
            view.updateDrawable(drawable)
        }

        override fun onLoadStarted(placeholder: Drawable?) {
            super.onLoadStarted(placeholder)
            setResourceInternal(null)
            setDrawable(placeholder)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            super.onLoadFailed(errorDrawable)
            setResourceInternal(null)
            setDrawable(errorDrawable)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            if (animatable != null) {
                animatable!!.stop()
            }
            setResourceInternal(null)
            setDrawable(placeholder)
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            if (transition == null || !transition.transition(resource, this)) {
                setResourceInternal(resource)
            } else {
                maybeUpdateAnimatable(resource)
            }
        }

        override fun onStart() {
            if (animatable != null) {
                animatable!!.start()
            }
        }

        override fun onStop() {
            if (animatable != null) {
                animatable!!.stop()
            }
        }

        private fun setResourceInternal(resource: Drawable?) {
            // Order matters here. Set the resource first to make sure that the Drawable has a valid and
            // non-null Callback before starting it.
            setResource(resource)
            maybeUpdateAnimatable(resource)
        }

        private fun maybeUpdateAnimatable(resource: Drawable?) {
            if (resource is Animatable) {
                animatable = resource
                animatable!!.start()
            } else {
                animatable = null
            }
        }

        private fun setResource(resource: Drawable?) {
            setDrawable(resource)
        }
    }

    /**
     * 更新Drawable
     */
    fun updateDrawable(drawable: Drawable?) {
        setImageDrawable(drawable)
        if (drawable == null) return
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