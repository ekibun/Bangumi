package soko.ekibun.bangumi.util

import okhttp3.*

object HttpUtil {
    var ua = ""
    var formhash = ""

    /**
     * 封装OkHttp请求，携带Cookie和User-Agent
     */
    fun getCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null, useCookie: Boolean = true): Call {
        val mutableHeader = header.toMutableMap()
        mutableHeader["User-Agent"] = header["User-Agent"] ?: ua
        val request = Request.Builder()
                .url(url)
                .headers(Headers.of(mutableHeader))
        if (body != null) request.post(body)
        val httpClient = OkHttpClient.Builder()
        if (useCookie) httpClient.cookieJar(WebViewCookieHandler())
        return httpClient.build().newCall(request.build())
    }
}