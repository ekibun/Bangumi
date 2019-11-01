package soko.ekibun.bangumi.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Request
import okhttp3.RequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException

object ApiHelper {
    fun <T> buildCallback(callback: (T) -> Unit, finish: (Throwable?) -> Unit = {}): Callback<T> {
        return object : Callback<T> {
            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e("errUrl", call.request().url().toString())
                if (!t.toString().contains("Canceled")) finish(t)
                t.printStackTrace()
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                Log.v("finUrl", call.request()?.url().toString())
                Log.v("finUrl", response.toString())
                finish(null)
                response.body()?.let { callback(it) }
            }
        }
    }

    enum class SaxEventType {
        NOTHING,
        BEGIN,
        END
    }

    fun parseWithSax(rsp: okhttp3.Response, checkEvent: (XmlPullParser, String) -> SaxEventType): String {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(rsp.body()!!.charStream())

        var lastData = ""
        loop@ while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (checkEvent(parser, lastData)) {
                SaxEventType.NOTHING -> {
                }
                SaxEventType.BEGIN -> {
                    lastData = ""
                }
                SaxEventType.END -> break@loop
            }
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    lastData += "<${parser.name} ${(0 until parser.attributeCount).joinToString(" ") { "${parser.getAttributeName(it)}=\"${parser.getAttributeValue(it)}\"" }}>"
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name != "br") lastData += "</${parser.name}>"
                }
                XmlPullParser.TEXT -> {
                    lastData += parser.text
                }
            }
            try {
                parser.next()
            } catch (e: Exception) {
            }
        }
        return lastData
    }

    fun <T> buildHttpCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null, useCookie: Boolean = true, converter: (okhttp3.Response) -> T): Call<T> {
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
                return okHttpCall.isExecuted
            }

            override fun clone(): Call<T> {
                return this
            }

            override fun isCanceled(): Boolean {
                return okHttpCall.isCanceled
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

    fun buildGroupCall(calls: Array<Call<*>>, onIndexResponse: (Int, Any?) -> Unit): Call<Unit> {
        return object : Call<Unit> {
            override fun enqueue(callback: Callback<Unit>) {
                var rspCount = 0
                calls.forEachIndexed { index, call ->
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