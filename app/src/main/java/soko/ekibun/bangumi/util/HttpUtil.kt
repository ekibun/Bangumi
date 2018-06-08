package soko.ekibun.bangumi.util

import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

object HttpUtil{
    private val okHttpClient = OkHttpClient()

    fun get(url: String, header: Map<String, String> = HashMap(), callback: Callback) {
        okHttpClient.newCall(Request.Builder()
                .url(url)
                .headers(Headers.of(header))
                .build())
                .enqueue(callback)
    }
}