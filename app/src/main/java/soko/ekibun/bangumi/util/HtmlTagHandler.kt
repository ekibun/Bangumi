package soko.ekibun.bangumi.util

import android.graphics.Color
import android.text.*
import org.xml.sax.XMLReader
import android.text.style.ImageSpan
import android.text.style.ClickableSpan
import android.view.View
import java.util.*
import android.text.style.RelativeSizeSpan
import android.widget.TextView

class HtmlTagHandler(private val widget: TextView, private var baseSize: Float = 13f, private val onClick:(ImageSpan)->Unit): Html.TagHandler{
    private val bgColor = widget.textColors.defaultColor
    private val colorInv = ResourceUtil.resolveColorAttr(widget.context, android.R.attr.textColorPrimaryInverse)

    override fun handleTag(openning: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag.toLowerCase(Locale.getDefault()) == "img") {
            val len = output.length
            val images = output.getSpans(len - 1, len, ImageSpan::class.java)
            output.setSpan(ClickableImage(images[0], onClick), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (tag.toLowerCase(Locale.getDefault()) == "size") {
            parseAttributes(xmlReader)
            if (openning) startSize(tag, output, xmlReader)
            else endSize(tag, output, xmlReader)
        }
        if (tag.toLowerCase(Locale.getDefault()) == "mask") {
            if (openning) startMask(tag, output, xmlReader)
            else endMask(tag, output, xmlReader)
        }
    }

    //size
    private var startSizeIndex = 0
    private var endSizeIndex = 0
    private fun startSize(tag: String, output: Editable, xmlReader: XMLReader) {
        startSizeIndex = output.length
    }
    private fun endSize(tag: String, output: Editable, xmlReader: XMLReader) {
        endSizeIndex = output.length
        var size = attributes["size"]?:""
        size = size.split("px".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        // 设置字体大小
        if (!TextUtils.isEmpty(size)) {
            output.setSpan(RelativeSizeSpan((size.toFloatOrNull()?:baseSize)/ baseSize), startSizeIndex, endSizeIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    //size
    private var startMaskIndex = 0
    private var endMaskIndex = 0
    private fun startMask(tag: String, output: Editable, xmlReader: XMLReader) {
        startMaskIndex = output.length
    }
    private fun endMask(tag: String, output: Editable, xmlReader: XMLReader) {
        endMaskIndex = output.length
        output.setSpan(MaskSpan(bgColor, colorInv, widget) {}, startMaskIndex, endMaskIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    class MaskSpan(private var bgColor: Int, private var colorInv: Int, private val textView: TextView, private val onClick:()->Unit): ClickableSpan(){
        var select = false
        override fun onClick(widget: View) {
            onClick()
            textView.tag = if(textView.tag == this) null else this
            textView.text = textView.text
        }
        override fun updateDrawState(ds: TextPaint) {
            ds.bgColor = bgColor
            ds.color = if(textView.tag == this) colorInv else Color.TRANSPARENT
            ds.isUnderlineText = false
        }
    }

    class ClickableImage(private val image: ImageSpan, private val onClick:(ImageSpan)->Unit): ClickableSpan(){
        override fun onClick(widget: View) {
            onClick(image)
        }
    }

    var attributes: HashMap<String, String> = HashMap()
    private fun parseAttributes(xmlReader: XMLReader) {
        try {
            val elementField = xmlReader.javaClass.getDeclaredField("theNewElement")
            elementField.isAccessible = true
            val element = elementField.get(xmlReader)
            val attsField = element.javaClass.getDeclaredField("theAtts")
            attsField.isAccessible = true
            val atts = attsField.get(element)
            val dataField = atts.javaClass.getDeclaredField("data")
            dataField.isAccessible = true
            val data = dataField.get(atts) as Array<String>
            val lengthField = atts.javaClass.getDeclaredField("length")
            lengthField.isAccessible = true
            val len = lengthField.get(atts) as Int

            for (i in 0 until len) {
                attributes[data[i * 5 + 1]] = data[i * 5 + 4]
            }
        } catch (e: Exception) {}
    }

}