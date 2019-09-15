package soko.ekibun.bangumi.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.TextUtils
import android.text.style.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.ui.topic.PostAdapter
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import kotlin.math.roundToInt

/**
 * Created by awarmisland on 2018/9/10.
 */
object TextUtil {

    /**
     * 将html转换为字符串
     */
    fun html2text(string: String): String {
        val doc = Jsoup.parse(string)
        doc.select("br").after("$\$b\$r$")
        return doc.body().text().replace("$\$b\$r$", "\n")
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
                    is PostAdapter.Companion.CustomURLSpan -> out.append("[url=${characterStyle.url}]")
                    is ImageSpan -> {
                        var source = characterStyle.source ?: (characterStyle.drawable as? ReplyDialog.UrlDrawable)?.url
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
                    is PostAdapter.Companion.CustomURLSpan -> out.append("[/url]")
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