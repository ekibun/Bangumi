package soko.ekibun.bangumi.api

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import android.util.Log
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException

object ApiHelper {
    fun <T> buildCallback(context: Context?, callback: (T)->Unit, finish:(Throwable?)->Unit={}): Callback<T> {
        return object:Callback<T>{
            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e("errUrl", call.request().url().toString())
                t.printStackTrace()
                if(!t.toString().contains("Canceled") && context is Activity)
                    Snackbar.make(context.window.decorView, "出错啦\n" + t.toString(), Snackbar.LENGTH_SHORT).show()
                finish(t)
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                finish(null)
                response.body()?.let { callback(it) }
            }
        }
    }

    fun <T> buildHttpCall(url: String, header: Map<String, String> = HashMap(), converter: (okhttp3.Response)->T): Call<T>{
        return object: retrofit2.Call<T>{
            private val retrofitCall = this
            val okHttpCall = HttpUtil.getCall(url, header)
            fun createResponse(response: okhttp3.Response): retrofit2.Response<T>{
                return retrofit2.Response.success(converter(response))
            }
            override fun enqueue(callback: retrofit2.Callback<T>) {
                okHttpCall.enqueue(object: okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        callback.onFailure(retrofitCall, e)
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val t = try{
                            createResponse(response)
                        }catch(e: Exception){
                            callback.onFailure(retrofitCall, e)
                            return
                        }
                        callback.onResponse(retrofitCall, t)
                    }
                })
            }
            override fun isExecuted(): Boolean { return okHttpCall.isExecuted }
            override fun clone(): retrofit2.Call<T> { return this }
            override fun isCanceled(): Boolean { return okHttpCall.isCanceled }
            override fun cancel() { okHttpCall.cancel() }
            override fun execute(): retrofit2.Response<T> {return createResponse(okHttpCall.execute()) }
            override fun request(): Request { return okHttpCall.request() }

        }
    }

    fun buildWebViewCall(webView: BackgroundWebView, url: String, header: Map<String, String> = HashMap()): Call<String>{
        return object: retrofit2.Call<String>{
            override fun enqueue(callback: retrofit2.Callback<String>) {
                webView.onCatchVideo={
                    Log.v("video", it.url.toString())
                    callback.onResponse(this, Response.success(it.url.toString()))
                    webView.onCatchVideo={}
                }
                val map = HashMap<String, String>()
                map["referer"]=url
                map.putAll(header)
                webView.loadUrl(url, map)
            }
            override fun isExecuted(): Boolean { return webView.url == "about:blank" }
            override fun clone(): retrofit2.Call<String> { return this }
            override fun isCanceled(): Boolean { return webView.url == "about:blank" }
            override fun cancel() { webView.loadUrl("about:blank") }
            override fun execute(): retrofit2.Response<String>? {return null }
            override fun request(): Request { return Request.Builder().url(url).build() }

        }
    }

    fun <T> buildGroupCall(calls: Array<Call<T>>): Call<T>{
        return object: retrofit2.Call<T>{
            override fun enqueue(callback: retrofit2.Callback<T>) { calls.forEach { it.enqueue(callback) } }
            override fun isExecuted(): Boolean { return calls.count { it.isExecuted } == calls.size }
            override fun clone(): retrofit2.Call<T> { return this }
            override fun isCanceled(): Boolean { return calls.count { it.isCanceled } == calls.size }
            override fun cancel() { calls.forEach { it.cancel() } }
            override fun execute(): retrofit2.Response<T>? {
                calls.forEach { it.execute() }
                return null }
            override fun request(): Request { return Request.Builder().build() }

        }
    }
}