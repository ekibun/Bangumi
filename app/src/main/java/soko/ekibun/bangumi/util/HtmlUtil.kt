package soko.ekibun.bangumi.util

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.util.Size
import android.view.View
import android.widget.EditText
import android.widget.TextView
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.topic.PhotoPagerAdapter
import soko.ekibun.bangumi.util.span.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

object HtmlUtil {

    private val bbcodeSpanFormatter = SpanFormatter.BBCodeFormatter()

    /**
     * 将Spanned转换为bbcode
     */
    fun span2bbcode(text: Spanned): String {
        return bbcodeSpanFormatter.format(text)
    }

    /**
     * 将bbcode转换为html
     */
    fun bbcode2html(text: String): String {
        return bbcodeSpanFormatter.bbcodeToHtml(text)
    }

    /**
     * 将html转换为字符串
     */
    fun html2text(string: String): String {
        return html2span(string).toString()
    }

    /**
     * 将html转换为Spanned
     */
    fun html2span(html: String, imageGetter: ImageGetter = ImageGetter()): Spanned {
        val doc = Jsoup.parse(html.replace(Regex("</?noscript>"), ""), Bangumi.SERVER)
        doc.outputSettings().indentAmount(0).prettyPrint(false)
        doc.select("script").remove()
        doc.select("img").forEach {
            if (!it.hasAttr("src")) it.remove()
        }
        doc.allElements.forEach { element ->
            arrayOf(element.childNodes()?.getOrNull(0), element.nextSibling()).filterIsInstance<TextNode>().forEach {
                val wholeText = it.wholeText.trimStart('\n')
                if (wholeText.isEmpty()) it.remove()
                else it.text(wholeText)
            }
        }
        return elementChildrenToSpan(doc.body(), imageGetter)
    }

    /**
     * 赋值并更新[TextView]引用
     */
    fun attachToTextView(span: Spanned, textView: TextView) {
        // 结束之前的请求
        (textView.text as? Spanned)?.let { text ->
            text.getSpans(0, text.length, UrlImageSpan::class.java).filterNot {
                span.getSpanFlags(it) == 0
            }.forEach {
                (it.drawable as? UrlDrawable)?.cancel()
            }
        }
        // 更新引用
        val weakRef = WeakReference(textView)
        span.getSpans(0, span.length, UrlImageSpan::class.java).forEach { imageSpan ->
            (imageSpan.drawable as? UrlDrawable)?.let {
                it.container = weakRef
                it.loadImage()
            }
        }
        span.getSpans(0, span.length, MaskSpan::class.java).forEach { maskSpan ->
            maskSpan.textView = weakRef
        }
        // TODO 只有图片的时候补一个零宽字符，不然只有图片的时候会吃掉lineSpacing
        textView.text = if (textView !is EditText && span.toString().trim('￼').isEmpty())
            SpannableStringBuilder("\u200B").append(span)
        else span
    }

    private const val LINE_BREAK = "\n"
    private fun elementChildrenToSpan(
        element: Element,
        imageGetter: ImageGetter
    ): SpannableStringBuilder {
        val span = SpannableStringBuilder()
        var endWithBlock: Boolean? = null

        element.childNodes().forEach { node ->
            span.append(
                when ((node as? Element)?.tagName()?.toLowerCase(Locale.ROOT)) {
                    "div" -> {
                        val lineBreak = if (endWithBlock == null) "" else LINE_BREAK
                        endWithBlock = true
                        SpannableStringBuilder(lineBreak).append(
                            elementChildrenToSpan(node, imageGetter).also {
                                parseBlockStyle(node, it)
                            })
                }
                else -> {
                    val lineBreak = if (endWithBlock == true) LINE_BREAK else ""
                    endWithBlock = false
                    SpannableStringBuilder(lineBreak).append(
                        if (node is Element) when (node.tagName()) {
                            "br" -> {
                                endWithBlock = null
                                "\n"
                            }
                            "img" -> {
                                val src = node.attr("src")
                                val sources = Bangumi.parseUrl(src)
                                val alt = node.attr("alt")
                                val isSmile = node.hasAttr("smileid")
                                val imageSpan = UrlImageSpan(
                                    imageGetter.getDrawable(sources),
                                    if (isSmile) alt else src
                                )
                                createImageSpan(imageSpan).also {
                                    if (!isSmile) {
                                        imageGetter.drawables.add(imageSpan.url)
                                        setSpan(
                                            ClickableImageSpan(imageSpan, imageGetter.onClick),
                                            it, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                    }
                                }
                            }
                            else -> elementChildrenToSpan(node, imageGetter).also {
                                parseSpanStyle(node, it)
                            }
                        } else (node as TextNode).wholeText)
                }
            })
        }
        return span
    }

    fun createImageSpan(imageSpan: UrlImageSpan): SpannableStringBuilder {
        return SpannableStringBuilder("￼").also {
            setSpan(imageSpan, it, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun parseBlockStyle(element: Element, span: SpannableStringBuilder) {
        parseSpanStyle(element, span)
        if (element.hasClass("quote")) setSpan(QuoteLineSpan(), span)
        if (element.hasClass("codeHighlight")) setSpan(CodeLineSpan(), span)
    }

    private const val BASE_FONT_SIZE = 12f
    private fun parseSpanStyle(element: Element, span: SpannableStringBuilder) {
        when (element.tagName().toLowerCase(Locale.ROOT)) {
            "a" -> setSpan(ClickableUrlSpan(element.attr("href")), span)
            "li" -> setSpan(BulletSpan(), span)
            "mask" -> setSpan(MaskSpan(), span)
            "b" -> setSpan(StyleSpan(Typeface.BOLD), span)
            "i" -> setSpan(StyleSpan(Typeface.ITALIC), span)
            "u" -> setSpan(UnderlineSpan(), span)
        }
        val style = element.attr("style")
            .toLowerCase(Locale.ROOT).split(";").filterNot { it.isEmpty() }
            .map { style -> style.split(":").let { it[0].trim() to it.getOrNull(1)?.trim() } }.toMap()
        if (style["font-weight"] == "bold") setSpan(StyleSpan(Typeface.BOLD), span)
        if (style["font-style"] == "italic") setSpan(StyleSpan(Typeface.ITALIC), span)
        if (style["text-decoration"] == "underline") setSpan(UnderlineSpan(), span)
        if (style["text-decoration"] == "line-through") setSpan(StrikethroughSpan(), span)
        if (style["font-size"] != null) setSpan(
            RelativeSizeSpan(
                (style["font-size"]?.replace("px", "")
                    ?.toFloatOrNull() ?: BASE_FONT_SIZE) / BASE_FONT_SIZE
            ), span
        )
        if (style["background-color"] == "#555") setSpan(MaskSpan(), span)
        else if (style["color"] != null) try {
            Color.parseColor(style["color"])
        } catch (e: IllegalArgumentException) {
            null
        }?.let {
            setSpan(ForegroundColorSpan((0xFF000000 or it.toLong()).toInt()), span)
        }
    }

    private fun setSpan(obj: Any, span: SpannableStringBuilder, type: Int = Spanned.SPAN_INCLUSIVE_EXCLUSIVE) {
        span.setSpan(obj, 0, span.length, type)
    }

    open class ImageGetter(
        val sizeCache: HashMap<String, Size> = HashMap(),
        val wrapWidth: (Float) -> Float = { it }
    ) {
        val drawables = ArrayList<String>()

        open val onClick: (View, UrlImageSpan) -> Unit = { itemView, span ->
            PhotoPagerAdapter.showWindow(
                itemView, drawables,
                index = drawables.indexOf(span.url)
            )
        }

        open fun createDrawable(): UrlDrawable {
            return UrlDrawable(wrapWidth, sizeCache)
        }

        fun getDrawable(source: String): UrlDrawable {
            val urlDrawable = createDrawable()
            urlDrawable.url = source
            sizeCache[source]?.let {
                urlDrawable.setBounds(0, 0, it.width, it.height)
            }
            return urlDrawable
        }
    }
}