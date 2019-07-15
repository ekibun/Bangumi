package soko.ekibun.bangumi.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException

object ApiHelper {
    fun <T> buildCallback(callback: (T)->Unit, finish:(Throwable?)->Unit={}): Callback<T> {
        return object:Callback<T>{
            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e("errUrl", call.request().url().toString())
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

    fun <T> buildHttpCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null, useCookie: Boolean = true, converter: (okhttp3.Response)->T): Call<T>{
        val uiHandler = Handler(Looper.getMainLooper())
        return object: Call<T>{
            private val retrofitCall = this
            val okHttpCall = HttpUtil.getCall(url, header, body, useCookie)
            fun createResponse(response: okhttp3.Response): Response<T>{
                return Response.success(converter(response))
            }
            override fun enqueue(callback: Callback<T>) {
                okHttpCall.enqueue(object: okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        uiHandler.post { callback.onFailure(retrofitCall, e) }
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val t = try{
                            createResponse(response)
                        }catch(e: Exception){
                            uiHandler.post { callback.onFailure(retrofitCall, e) }
                            return
                        }
                        uiHandler.post { callback.onResponse(retrofitCall, t) }
                    }
                })
            }
            override fun isExecuted(): Boolean { return okHttpCall.isExecuted }
            override fun clone(): Call<T> { return this }
            override fun isCanceled(): Boolean { return okHttpCall.isCanceled }
            override fun cancel() { okHttpCall.cancel() }
            override fun execute(): Response<T> {return createResponse(okHttpCall.execute()) }
            override fun request(): Request { return okHttpCall.request() }

        }
    }
}