package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView

open class BigImageView@JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    /*
    val mSsiv: SubsamplingScaleImageView by lazy {
        val ssiv = SubsamplingScaleImageView(context)
        ssiv.setDoubleTapZoomDuration(250)
        ssiv
    }
    val mPhotoView: PhotoView by lazy {
        val pv = PhotoView(context)
        pv.mediumScale = pv.maximumScale
        pv
    }

    val isMinScale get() = drawable?.let{
        if(drawable is BitmapDrawable) mSsiv.scale == mSsiv.minScale else mPhotoView.scale == 1f
    }?: true
    */
    val isMinScale get() = scale == 1f

    val glideTarget = MyViewTarget(this)

    class MyViewTarget(view: BigImageView) : ViewTarget<BigImageView, Drawable>(view), Transition.ViewAdapter {
        private var animatable: Animatable? = null

        /**
         * Returns the current [android.graphics.drawable.Drawable] being displayed in the view
         * using [android.widget.ImageView.getDrawable].
         */
        override fun getCurrentDrawable(): Drawable? {
            return view.drawable
        }

        /**
         * Sets the given [android.graphics.drawable.Drawable] on the view using [ ][android.widget.ImageView.setImageDrawable].
         *
         * @param drawable {@inheritDoc}
         */
        override fun setDrawable(drawable: Drawable?) {
            view.updateDrawable(drawable)
        }

        /**
         * Sets the given [android.graphics.drawable.Drawable] on the view using [ ][android.widget.ImageView.setImageDrawable].
         *
         * @param placeholder {@inheritDoc}
         */
        override fun onLoadStarted(placeholder: Drawable?) {
            super.onLoadStarted(placeholder)
            setResourceInternal(null)
            setDrawable(placeholder)
        }

        /**
         * Sets the given [android.graphics.drawable.Drawable] on the view using [ ][android.widget.ImageView.setImageDrawable].
         *
         * @param errorDrawable {@inheritDoc}
         */
        override fun onLoadFailed(errorDrawable: Drawable?) {
            super.onLoadFailed(errorDrawable)
            setResourceInternal(null)
            setDrawable(errorDrawable)
        }

        /**
         * Sets the given [android.graphics.drawable.Drawable] on the view using [ ][android.widget.ImageView.setImageDrawable].
         *
         * @param placeholder {@inheritDoc}
         */
        override fun onLoadCleared(placeholder: Drawable?) {
            super.onLoadCleared(placeholder)
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

        fun setResource(resource: Drawable?){
            setDrawable(resource)
        }
    }

    //var drawable: Drawable? = null
    fun updateDrawable(drawable: Drawable?){
        setImageDrawable(drawable)
        if(drawable == null) return
        val w = drawable.intrinsicWidth * 1f
        val h = drawable.intrinsicHeight* 1f
        val W = measuredWidth* 1f
        val H = measuredHeight* 1f
        if(w <= 0 || h <= 0 || W <= 0 || H <= 0) return
        val minScale = Math.min(H/h, W/w)
        val maxScale = Math.max(H/h, W/w)
        val dif = maxScale / minScale
        when {
            dif > 1f -> setScaleLevels(1f, dif, Math.max(1f + (dif - 1f) * 2, 3f))
            else -> setScaleLevels(1f, 1.75f, 3f)
        }
        /*
        removeAllViews()
        this.drawable= drawable
        if(drawable == null) return
        if(drawable is BitmapDrawable){
            //ssiv
            mSsiv.setImage(ImageSource.bitmap(drawable.bitmap))
            addView(mSsiv, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }else{
            //photoview
            mPhotoView.setImageDrawable(drawable)
            addView(mPhotoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        */
    }
}