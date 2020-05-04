package soko.ekibun.bangumi.util.span

import android.text.style.ClickableSpan
import android.util.Log
import android.view.View

/**
 *
 */
class ClickableImageSpan(
    var image: UrlImageSpan,
    private val onClick: (View, UrlImageSpan) -> Unit
) : ClickableSpan() {
    override fun onClick(widget: View) {
        Log.v("click", image.drawable.toString())
        val drawable = image.drawable
        if (drawable is UrlDrawable) {
            if (drawable.error == true) drawable.loadImage()
            else if (drawable.error == false) onClick(widget, image)
        } else onClick(widget, image)
    }

}