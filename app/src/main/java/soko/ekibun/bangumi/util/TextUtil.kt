package soko.ekibun.bangumi.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.*
import android.view.View
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import kotlin.math.roundToInt

/**
 * Created by awarmisland on 2018/9/10.
 */
object TextUtil {
    /**
     * 给链接加上回调
     */
    fun setTextUrlCallback(htmlString: Spanned, onClick: (String) -> Unit): Spanned {
        val objs = (htmlString as? SpannableStringBuilder)?.getSpans(0, htmlString.length, URLSpan::class.java)
                ?: return htmlString
        if (objs.isNotEmpty()) for (obj in objs) {
            val start = htmlString.getSpanStart(obj)
            val end = htmlString.getSpanEnd(obj)
            if (obj is URLSpan) {
                val url = obj.url
                htmlString.removeSpan(obj)
                htmlString.setSpan(CustomURLSpan(url, onClick), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
        return htmlString
    }

    /**
     * Url回调Span
     */
    class CustomURLSpan(val url: String, val onClick: (String) -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            onClick(url)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = ds.linkColor
            ds.isUnderlineText = false
        }
    }


    /**
     * 将html转换为字符串
     */
    fun html2text(string: String): String {
        val doc = Jsoup.parse(string)
        doc.select("br").after("$\$b\$r$")
        return doc.body().text().replace("$\$b\$r$", "\n")
    }

    /**
     * 将bbcode转换为html
     */
    fun bbcode2html(text: String): String {
        return text.replace("\n", "<br/>").replace(Regex("""\[(/?(b|i|u|mask))]"""), "<$1>")
                .replace("[s]", "<span style=\"text-decoration:line-through\">")
                .replace(Regex("""\[url](.*?)\[/url]"""), "<a href=\"$1\"></a>")
                .replace(Regex("""\[url=(.*?)]"""), "<a href=\"$1\">")
                .replace(Regex("""\[/url]"""), "</a>")
                .replace(Regex("""\[img](.*?)\[/img]"""), "<img src=\"$1\"/>")
                .replace(Regex("""\[color=(.*?)]"""), "<span style=\"color:$1\">")
                .replace(Regex("""\[size=(.*?)]"""), "<span style=\"font-size:$1px\">")
                .replace(Regex("""\[(/color|/s|/size)]"""), "</span>").let {
                    var ret = it
                    ReplyDialog.emojiList.forEach {
                        ret = ret.replace(it.first, "<img src=\"${it.second.replace(Bangumi.SERVER, "")}\"/>")
                    }
                    ret
                }
    }

    /**
     * 将span转换为bbcode
     */
    fun span2bbcode(text: Spanned): String {
        val out = StringBuilder()
        val start = 0
        val end = text.length
        var next: Int
        var i = start
        while (i < end) {
            next = TextUtils.indexOf(text, '\n', i, end)
            if (next < 0) next = end

            var nl = 0
            while (next < end && text[next] == '\n') {
                nl++
                next++
            }
            withinParagraph(out, text, i, next - nl)
            //支持换行
            for (l in 0 until nl) out.append("\n")
            i = next
        }
        return out.toString()
    }

    private fun withinParagraph(out: StringBuilder, text: Spanned, start: Int, end: Int) {
        var i = start
        while (i < end) {
            val next = text.nextSpanTransition(i, end, CharacterStyle::class.java)
            val style = text.getSpans(i, next, CharacterStyle::class.java)
            for (characterStyle in style) {
                when (characterStyle) {
                    is StyleSpan -> {
                        val s = characterStyle.style
                        if (s and Typeface.BOLD != 0) out.append("[b]")
                        if (s and Typeface.ITALIC != 0) out.append("[i]")
                    }
                    is UnderlineSpan -> out.append("[u]")
                    is StrikethroughSpan -> out.append("[s]")
                    is CustomURLSpan -> out.append("[url=${characterStyle.url}]")
                    is URLSpan -> out.append("[url=${characterStyle.url}]")
                    is ImageSpan -> {
                        var source = characterStyle.source
                                ?: (characterStyle.drawable as? UrlDrawable)?.url
                        if (source != null && source.startsWith("/img/smiles/"))
                            source = ReplyDialog.emojiList.firstOrNull { it.second.contains(source!!) }?.first
                        if (source != null && source.startsWith("("))
                            out.append(source)
                        else if (source != null) out.append("[img]$source[/img]")
                        i = next // Don't output the dummy character underlying the image.
                    }
                    is RelativeSizeSpan -> out.append("[size=${(characterStyle.sizeChange * 12).roundToInt()}]")
                    is ForegroundColorSpan -> out.append(String.format("[color=#%06X]", 0xFFFFFF and characterStyle.foregroundColor))
                    is HtmlTagHandler.MaskSpan -> out.append("[mask]")
                }
            }
            withinStyle(out, text, i, next)
            for (j in style.indices.reversed()) {
                when (style[j]) {
                    is HtmlTagHandler.MaskSpan -> out.append("[/mask]")
                    is ForegroundColorSpan -> out.append("[/color]")
                    is RelativeSizeSpan -> out.append("[/size]")
                    is CustomURLSpan -> out.append("[/url]")
                    is URLSpan -> out.append("[/url]")
                    is StrikethroughSpan -> out.append("[/s]")
                    is UnderlineSpan -> out.append("[/u]")
                    is StyleSpan -> {
                        val s = (style[j] as StyleSpan).style
                        if (s and Typeface.ITALIC != 0) out.append("[/i]")
                        if (s and Typeface.BOLD != 0) out.append("[/b]")
                    }
                }
            }
            i = next
        }
    }

    private fun withinStyle(out: StringBuilder, text: CharSequence, start: Int, end: Int) {
        out.append(text.subSequence(start, end))
    }
}