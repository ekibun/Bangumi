package soko.ekibun.bangumi.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.TextUtils
import android.text.style.*
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import soko.ekibun.bangumi.util.span.*
import kotlin.math.roundToInt

abstract class SpanFormatter {
    abstract fun format(span: Any, inner: () -> String): String

    fun format(text: Spanned): String {
        return spanFormat(text, 0, text.length, listOf())
    }

    private fun spanFormat(text: Spanned, start: Int, end: Int, ignoreSpan: List<Any>): String {
        val out = StringBuilder()
        var i = start
        while (i < end) {
            var next = text.nextSpanTransition(i, end, ParagraphStyle::class.java)
            val style = text.getSpans(i, next, ParagraphStyle::class.java)
            val p = style.filter {
                it !in ignoreSpan && text.getSpanStart(it) == i
            }.maxBy { text.getSpanEnd(it) }
            if (p != null) {
                next = text.getSpanEnd(p)
                out.append(format(p) { spanFormat(text, i, next, ignoreSpan.plus(p)) })
            } else out.append(withinParagraph(text, i, next, ignoreSpan))
            i = next
        }
        return out.toString()
    }

    private fun withinParagraph(text: Spanned, start: Int, end: Int, ignoreSpan: List<Any>): String {
        val out = StringBuilder()
        var i = start
        while (i < end) {
            var next = text.nextSpanTransition(i, end, CharacterStyle::class.java)
            val style = text.getSpans(i, next, CharacterStyle::class.java)
            val c = style.filter {
                it !in ignoreSpan && text.getSpanStart(it) == i
            }.maxBy { text.getSpanEnd(it) }
            if (c != null) {
                next = text.getSpanEnd(c)
                out.append(format(c) { withinParagraph(text, i, next, ignoreSpan.plus(c)) })
            } else out.append(text.substring(i, next))
            i = next
        }
        return out.toString()
    }

    class BBCodeFormatter : SpanFormatter() {
        override fun format(span: Any, inner: () -> String): String {
            when (span) {
                is QuoteLineSpan -> return "[quote]${inner()}[/quote]"
                is CodeLineSpan -> return "[code]${inner()}[/code]"
                is BulletSpan -> return "[*]${inner()}"
                is StyleSpan -> {
                    val s = span.style
                    if (s and Typeface.BOLD != 0) return "[b]${inner()}[/b]"
                    if (s and Typeface.ITALIC != 0) return "[i]${inner()}[/i]"
                }
                is UnderlineSpan -> return "[u]${inner()}[/u]"
                is StrikethroughSpan -> return "[s]${inner()}[/s]"
                is ClickableUrlSpan -> return "[url=${span.url}]${inner()}[/url]"
                is RelativeSizeSpan -> return "[size=${(span.sizeChange * 12).roundToInt()}]${inner()}[/size]"
                is ForegroundColorSpan -> return "${
                String.format("[color=#%06X]", 0xFFFFFF and span.foregroundColor)}${inner()}[/color]"
                is MaskSpan -> return "[mask]${inner()}[/mask]"
                is ImageSpan -> return if (span.source?.startsWith("(") == true) span.source!!
                else "[img]${span.source ?: (span.drawable as? UrlDrawable)?.url}[/img]"
            }
            return inner()
        }

        fun bbcodeToHtml(text: String): String {
            return TextUtils.htmlEncode(text).replace(" ", "&nbsp;")
                .replace(Regex("""\[quote]"""), "<div class=\"quote\">")
                .replace(Regex("""\[code]"""), "<div class=\"codeHighlight\">")
                .replace(Regex("""\[/(quote|code)]\n?"""), "</div>")
                .replace("\n", "<br/>")
                .replace(Regex("""\[(/?(b|i|u|mask))]"""), "<$1>")
                .replace("[s]", "<span style=\"text-decoration:line-through\">")
                .replace(Regex("""\[url](.*?)\[/url]"""), "<a href=\"$1\"></a>")
                .replace(Regex("""\[url=(.*?)]"""), "<a href=\"$1\">")
                .replace(Regex("""\[/url]"""), "</a>")
                .replace(Regex("""\[img](.*?)\[/img]"""), "<img src=\"$1\"/>")
                .replace(Regex("""\[color=(.*?)]"""), "<span style=\"color:$1\">")
                .replace(Regex("""\[size=(.*?)]"""), "<span style=\"font-size:$1px\">")
                .replace(Regex("""\[/(color|s|size)]"""), "</span>").let {
                    var ret = it
                    ReplyDialog.emojiList.forEach {
                        ret = ret.replace(
                            it.first,
                            "<img src=\"${it.second.replace(Bangumi.SERVER, "")}\" smileid alt=\"${it.first}\"/>"
                        )
                    }
                    ret
                }
        }
    }
}