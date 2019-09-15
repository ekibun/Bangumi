package soko.ekibun.bangumi.util

import okhttp3.*
import org.jsoup.Jsoup

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

    /**
     * 将html转换为字符串
     */
    fun html2text(string: String): String {
        val doc = Jsoup.parse(string)
        doc.select("br").after("$\$b\$r$")
        return doc.body().text().replace("$\$b\$r$", "\n")
    }
}