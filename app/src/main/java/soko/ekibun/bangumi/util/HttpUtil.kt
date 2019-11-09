package soko.ekibun.bangumi.util

import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.http.BridgeInterceptor

object HttpUtil {
    var ua = ""
    var formhash = ""
    val httpCookieClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .addInterceptor(BrotliInterceptor)
                .addInterceptor(BridgeInterceptor(WebViewCookieHandler())).build()
    }

    /**
     * 封装OkHttp请求，携带Cookie和User-Agent
     */
    fun getCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null, useCookie: Boolean = true): Call {
        val mutableHeader = header.toMutableMap()
        mutableHeader["User-Agent"] = header["User-Agent"] ?: ua
        val request = Request.Builder()
                .url(url)
                .headers(mutableHeader.toHeaders())
        if (body != null) request.post(body)
        val httpClient = if (useCookie) httpCookieClient else OkHttpClient()
        return httpClient.newCall(request.build())
    }
}