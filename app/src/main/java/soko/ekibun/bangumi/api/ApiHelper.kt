package soko.ekibun.bangumi.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Xml
import android.widget.Toast
import okhttp3.Request
import okhttp3.RequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException
import java.io.Reader

/**
 * API工具库
 */
object ApiHelper {
    /**
     * 创建回调
     * @param callback Function1<T, Unit>
     * @param finish Function1<Throwable?, Unit>
     * @return Callback<T>
     */
    fun <T> buildCallback(callback: (T) -> Unit, finish: (Throwable?) -> Unit = {}): Callback<T> {
        return object : Callback<T> {
            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e("errUrl", call.request().url.toString())
                if (!t.toString().contains("Canceled")) {
                    Toast.makeText(App.app, t.message, Toast.LENGTH_SHORT).show()
                    finish(t)
                    Log.e("call", Log.getStackTraceString(t))
                }
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                Log.v("finUrl", call.request()?.url.toString())
                Log.v("finUrl", response.toString())
                finish(null)
                response.body()?.let { callback(it) }
            }
        }
    }

    /**
     * 转换Call返回的数据类型
     * @param src Call<T>
     * @param converter Function1<T, U>
     * @return Call<U>
     */
    fun <T, U> convertCall(src: Call<T>, converter: (T) -> U): Call<U> {
        return object : Call<U> {
            override fun execute(): Response<U> {
                val t = src.execute()
                return if (t.isSuccessful) {
                    Response.success(converter(t.body()!!))
                } else
                    Response.error(t.code(), t.errorBody()!!)
            }

            override fun enqueue(callback: Callback<U>) {
                src.enqueue(buildCallback({
                    callback.onResponse(clone(), Response.success(converter(it)!!))
                }, {
                    if (it != null) callback.onFailure(clone(), it)
                }))
            }

            override fun isExecuted(): Boolean {
                return src.isExecuted
            }

            override fun clone(): Call<U> {
                return this
            }

            override fun isCanceled(): Boolean {
                return src.isCanceled
            }

            override fun cancel() {
                src.cancel()
            }

            override fun request(): Request? {
                return src.request()
            }
        }
    }

    /**
     * Sax事件
     */
    enum class SaxEventType {
        NOTHING,
        BEGIN,
        END
    }

    /**
     * Sax解析
     * @param rsp Response
     * @param checkEvent Function2<XmlPullParser, String, SaxEventType>
     * @return String
     */
    fun parseWithSax(rsp: okhttp3.Response, checkEvent: (XmlPullParser, () -> String) -> SaxEventType): String {
        val parser = XmlPullParserFactory.newInstance().apply {
            this.isValidating = false
            this.setFeature(Xml.FEATURE_RELAXED, true)
            this.isNamespaceAware = false
        }.newPullParser()
        val stream = rsp.body!!.charStream()

        val charList = StringBuilder()
        var lastLineIndex = 0
        var lastClipIndex = 0
        val lineMap = arrayListOf(0, 0)

        parser.setInput(object : Reader() {
            override fun close() {
                stream.close()
            }

            override fun read(p0: CharArray, p1: Int, p2: Int): Int {
                val ret = stream.read(p0, p1, p2)
                if (ret > 0) {
                    charList.append(p0, p1, ret)
                    while (true) {
                        val nextLineIndex = charList.indexOf('\n', lastLineIndex - lastClipIndex + 1)
                        if (nextLineIndex < 0) break
                        lastLineIndex = nextLineIndex + lastClipIndex
                        lineMap.add(lastLineIndex)
                    }
                }
                return if (ret >= p1) ret - p1 else ret
            }
        })


        var lastEventLineNumber = 0
        var lastEventColumnNumber = 0
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = checkEvent(parser) {
                charList.substring(
                    0,
                    lineMap[parser.lineNumber] + parser.columnNumber - lastClipIndex
                )
            }
            if (event == SaxEventType.BEGIN) {
                val curIndex = lineMap[lastEventLineNumber] + lastEventColumnNumber
                charList.delete(0, curIndex - lastClipIndex)
                lastClipIndex = curIndex
            } else if (event == SaxEventType.END) break
            lastEventLineNumber = parser.lineNumber
            lastEventColumnNumber = parser.columnNumber
            try {
                parser.next()
            } catch (e: Exception) { /* no-op */
            }
        }
        return charList.toString()
    }

    /**
     * 创建http请求
     * @param url String
     * @param header Map<String, String>
     * @param body RequestBody?
     * @param useCookie Boolean
     * @param converter Function1<Response, T>
     * @return Call<T>
     */
    fun <T> buildHttpCall(
        url: String,
        header: Map<String, String> = HashMap(),
        body: RequestBody? = null,
        useCookie: Boolean = true,
        converter: (okhttp3.Response) -> T
    ): Call<T> {
        val uiHandler = Handler(Looper.getMainLooper())
        return object : Call<T> {
            private val retrofitCall = this
            val okHttpCall = HttpUtil.getCall(url, header, body, useCookie)
            fun createResponse(response: okhttp3.Response): Response<T> {
                return Response.success(converter(response))
            }

            override fun enqueue(callback: Callback<T>) {
                okHttpCall.enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        uiHandler.post { callback.onFailure(retrofitCall, e) }
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val t = try {
                            createResponse(response)
                        } catch (e: Exception) {
                            uiHandler.post { callback.onFailure(retrofitCall, e) }
                            return
                        }
                        uiHandler.post { callback.onResponse(retrofitCall, t) }
                    }
                })
            }

            override fun isExecuted(): Boolean {
                return okHttpCall.isExecuted()
            }

            override fun clone(): Call<T> {
                return this
            }

            override fun isCanceled(): Boolean {
                return okHttpCall.isCanceled()
            }

            override fun cancel() {
                okHttpCall.cancel()
            }

            override fun execute(): Response<T> {
                return createResponse(okHttpCall.execute())
            }

            override fun request(): Request {
                return okHttpCall.request()
            }

        }
    }

    /**
     * 并行请求
     * @param calls Array<Call<*>>
     * @param onIndexResponse Function2<Int, Any?, Unit>
     * @return Call<Unit>
     */
    fun buildGroupCall(calls: Array<Call<*>>, onIndexResponse: (Int, Any?) -> Unit): Call<Unit> {
        return object : Call<Unit> {
            override fun enqueue(callback: Callback<Unit>) {
                var rspCount = 0
                calls.forEachIndexed { index, call ->
                    @Suppress("UNCHECKED_CAST")
                    (call as Call<Any>).enqueue(buildCallback({
                        onIndexResponse(index, it)
                    }, {
                        rspCount++
                        if (rspCount == calls.size) callback.onResponse(this, Response.success(null))
                    }))
                }
            }

            override fun isExecuted(): Boolean {
                return calls.count { it.isExecuted } == calls.size
            }

            override fun clone(): Call<Unit> {
                return this
            }

            override fun isCanceled(): Boolean {
                return calls.any { it.isCanceled }
            }

            override fun cancel() {
                calls.forEach { it.cancel() }
            }

            override fun execute(): Response<Unit>? {
                calls.forEach { it.execute()?.body() }
                return null
            }

            override fun request(): Request? {
                return null
            }

        }
    }
}