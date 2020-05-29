package soko.ekibun.bangumi.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.http.BridgeInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Http请求工具类
 */
object HttpUtil {
    val ua =
        "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Mobile Safari/537.36"
    var formhash = ""
    private val cookieHandler by lazy { WebViewCookieHandler() }
    private val httpClients: Array<OkHttpClient> by lazy {
        arrayOf(
            httpClientBuilder.addInterceptor(BridgeInterceptor(cookieHandler))
                .followRedirects(false)
                .followSslRedirects(false).build(),
            httpClientBuilder.addInterceptor(BridgeInterceptor(cookieHandler)).build(),
            httpClientBuilder.followRedirects(false)
                .followSslRedirects(false).build(),
            httpClientBuilder.build()
        )
    }

    private val httpClientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(BrotliInterceptor)
            .addNetworkInterceptor(HttpLoggingInterceptor().apply { this.level = HttpLoggingInterceptor.Level.BASIC })
    }

    /**
     * 请求options
     */
    data class RequestOption(
        val header: Map<String, String> = HashMap(),
        val body: RequestBody? = null,
        val useCookie: Boolean = true,
        val followRedirect: Boolean = true
    )

    /**
     * 封装OkHttp请求，携带Cookie和User-Agent
     */
    suspend fun fetch(url: String, options: RequestOption = RequestOption()): Response {
        return suspendCancellableCoroutine { continuation ->
            val call = getCall(url, options)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

            })

            continuation.invokeOnCancellation {
                try {
                    call.cancel()
                } catch (ex: Throwable) {
                    //Ignore cancel exception
                }
            }
        }
    }

    private fun getCall(url: String, options: RequestOption = RequestOption()): Call {
        val mutableHeader = options.header.toMutableMap()
        mutableHeader["User-Agent"] = options.header["User-Agent"] ?: ua
        val request = Request.Builder()
            .url(url)
            .headers(mutableHeader.toHeaders())
        if (options.body != null) request.post(options.body)
        return httpClients[(if (options.useCookie) 0 else 2) + (if (options.followRedirect) 1 else 0)].newCall(request.build())
    }
}