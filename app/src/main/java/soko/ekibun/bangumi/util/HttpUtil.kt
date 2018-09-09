package soko.ekibun.bangumi.util

import okhttp3.*
import java.net.URI

object HttpUtil {

    fun get(url: String, header: Map<String, String> = HashMap(), callback: Callback) {
        OkHttpClient().newCall(Request.Builder()
                .url(url)
                .headers(Headers.of(header))
                .build())
                .enqueue(callback)
    }

    fun getCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null): Call {
        val request = Request.Builder()
                .url(url)
                .headers(Headers.of(header))
        if (body != null)
            request.post(body)
        return OkHttpClient().newCall(request.build())
    }

    fun getUrl(url: String, baseUri: URI?): String{
        return baseUri?.resolve(url)?.toASCIIString() ?: URI.create(url).toASCIIString()
    }
}