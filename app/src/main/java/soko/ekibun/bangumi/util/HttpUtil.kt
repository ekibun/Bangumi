package soko.ekibun.bangumi.util

import okhttp3.*

object HttpUtil{
    private val okHttpClient = OkHttpClient()

    fun get(url: String, header: Map<String, String> = HashMap(), callback: Callback) {
        okHttpClient.newCall(Request.Builder()
                .url(url)
                .headers(Headers.of(header))
                .build())
                .enqueue(callback)
    }

    fun getCall(url: String, header: Map<String, String> = HashMap()): Call {
        return okHttpClient.newCall(Request.Builder()
                .url(url)
                .headers(Headers.of(header))
                .build())
    }
}