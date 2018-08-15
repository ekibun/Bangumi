package soko.ekibun.bangumi.util

import android.text.Editable
import android.text.Html
import org.xml.sax.XMLReader
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.ClickableSpan
import android.view.View
import java.util.*


class ImageTagHandler(private val onClick:(ImageSpan)->Unit): Html.TagHandler{
    override fun handleTag(openning: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag.toLowerCase(Locale.getDefault()) == "img") {
            val len = output.length
            val images = output.getSpans(len - 1, len, ImageSpan::class.java)
            output.setSpan(ClickableImage(images[0], onClick), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    }

    class ClickableImage(private val image: ImageSpan, private val onClick:(ImageSpan)->Unit): ClickableSpan(){
        override fun onClick(widget: View) {
            onClick(image)
        }
    }

}