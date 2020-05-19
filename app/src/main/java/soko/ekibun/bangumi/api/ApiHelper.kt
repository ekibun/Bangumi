package soko.ekibun.bangumi.api

import kotlinx.coroutines.*

/**
 * API工具库
 */
object ApiHelper {
    /**
     * Sax事件
     */
    enum class SaxEventType {
        NOTHING,
        BEGIN,
        END
    }

    private val tagMatcher = "<(div|li)(.+?)>".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Sax解析
     * @param rsp Response
     * @param checkEvent Function2<Element, String, SaxEventType>
     * @return String
     */
    fun parseSax(rsp: okhttp3.Response, checkEvent: (String, String, () -> String) -> SaxEventType): String {
        val stream = rsp.body!!.charStream()
        val chars = StringBuilder()
        val buffer = CharArray(8192)
        var lastLineIndex = 0
        var lastClipIndex = 0
        outer@ while (true) {
            val len = stream.read(buffer)
            if (len < 0) break
            chars.append(buffer, 0, len)
            val findLastClipIndex = lastClipIndex
            for (match in tagMatcher.findAll(chars, lastLineIndex - lastClipIndex)) {
                lastLineIndex = match.range.last + findLastClipIndex + 1
                val curIndex = match.range.first + findLastClipIndex
                val event = checkEvent(match.groupValues[1], match.groupValues[2]) {
                    chars.substring(lastClipIndex - findLastClipIndex, curIndex - findLastClipIndex)
                }
                if (event == SaxEventType.BEGIN) {
                    lastClipIndex = curIndex
                } else if (event == SaxEventType.END) break@outer
            }
            chars.delete(0, lastClipIndex - findLastClipIndex)
        }
        return chars.toString()
    }

    suspend fun parseSaxAsync(
        rsp: okhttp3.Response,
        checkEvent: suspend (String, String) -> Pair<Any?, SaxEventType>,
        runAsync: suspend CoroutineScope.(Any, String) -> Unit
    ): String {
        return coroutineScope {
            val ret = ArrayList<Deferred<Unit>>()
            val stream = rsp.body!!.charStream()
            val chars = StringBuilder()
            val buffer = CharArray(8192)
            var lastLineIndex = 0
            var lastClipIndex = 0
            var end = false
            outer@ while (!end) {
                val len = withContext(Dispatchers.IO) { stream.read(buffer) }
                if (len < 0) break
                chars.append(buffer, 0, len)
                val findLastClipIndex = lastClipIndex

                parseTag(chars, lastLineIndex - lastClipIndex) { start, last, tagName, attrs ->
                    lastLineIndex = last + findLastClipIndex
                    val curIndex = start + findLastClipIndex
                    val (tag, event) = checkEvent.invoke(tagName, attrs)
                    if (tag != null) {
                        val str = chars.substring(lastClipIndex - findLastClipIndex, curIndex - findLastClipIndex)
                        ret += async { runAsync(tag, str) }
                    }
                    if (event == SaxEventType.BEGIN) {
                        lastClipIndex = curIndex
                    } else if (event == SaxEventType.END) {
                        end = true
                    }
                    end
                }
                chars.delete(0, lastClipIndex - findLastClipIndex)
            }
            ret.awaitAll()
            chars.toString()
        }
    }

    private suspend fun parseTag(
        char: StringBuilder,
        start: Int,
        callback: suspend (Int, Int, String, String) -> Boolean
    ) {
        var index = start
        while (true) {
            index = char.indexOf('<', index) + 1
            if (index == 0 || index >= char.length) break
            if (char[index] == '/') continue
            val indexEnd = char.indexOf('>', index + 1)
            if (indexEnd < 0) break
            val tagEnd = char.indexOf(' ', index + 1)
            if (tagEnd in 1 until indexEnd) {
                if (callback(
                        index - 1,
                        indexEnd,
                        char.substring(index, tagEnd),
                        char.substring(tagEnd, indexEnd)
                    )
                ) break
            }
            index = indexEnd
        }
    }
}