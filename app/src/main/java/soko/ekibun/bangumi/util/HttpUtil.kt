package soko.ekibun.bangumi.util

import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.http.BridgeInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Http请求工具类
 */
object HttpUtil {
    val ua = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Mobile Safari/537.36"
    var formhash = ""
    private val httpCookieClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
//            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(BrotliInterceptor)
            .addNetworkInterceptor(HttpLoggingInterceptor().apply { this.level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor(BridgeInterceptor(WebViewCookieHandler())).build()
    }

    /**
     * 封装OkHttp请求，携带Cookie和User-Agent
     * @param url String
     * @param header Map<String, String>
     * @param body RequestBody?
     * @param useCookie Boolean
     * @return Call
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