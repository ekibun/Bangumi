package soko.ekibun.bangumi.util.span

import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.View

/**
 *
 */
class ClickableImageSpan(
    var image: ImageSpan,
    private val onClick: (View, ImageSpan) -> Unit
) : ClickableSpan() {
    override fun onClick(widget: View) {
        Log.v("click", this.toString())
        val drawable = image.drawable
        if (drawable is UrlDrawable) {
            if (drawable.error == true) drawable.loadImage()
            else if (drawable.error == false) onClick(widget, image)
        } else onClick(widget, image)
    }

}