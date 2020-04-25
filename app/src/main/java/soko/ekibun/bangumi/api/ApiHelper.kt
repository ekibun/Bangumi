package soko.ekibun.bangumi.api

import android.util.Xml
import android.widget.Toast
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import okhttp3.RequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException
import java.io.Reader

/**
 * API工具库
 */
object ApiHelper {

    /**
     * 包装Retrofit Builder
     * - 默认带上[GsonConverterFactory]和[RxJava3CallAdapterFactory]
     */
    fun createRetrofitBuilder(baseUrl: String): Retrofit.Builder {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createAsync())
    }

    /**
     * 在主线程回调
     * - 加入[observables]便于在[Activity.onDestroy][soko.ekibun.bangumi.ui.view.BaseActivity.onDestroy]中清除所有请求
     * - `onError`中调用`onComplete`保持协同
     * - `onError`默认弹出[Toast]:
     *    ```
     *    Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
     *    ```
     */
    fun <T> Observable<T>.subscribeOnUiThread(
        onNext: (t: T) -> Unit,
        onError: (t: Throwable) -> Unit = {
            Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
        },
        onComplete: () -> Unit = {},
        key: String? = null
    ): Disposable {
        if (key != null) observablesWithKeys.remove(key)?.dispose()
        return this.observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, {
                onError(it)
                onComplete()
            }, onComplete).also {
                observables.add(it)
                if (key != null) observablesWithKeys[key] = it
            }
    }

    private val observablesWithKeys = HashMap<String, Disposable>()
    private val observables = CompositeDisposable()

    /**
     * 清空[observables]
     */
    fun clearObservables() {
        observables.clear()
    }

    /**
     * 创建OkHttp的[Observable]
     */
    fun createHttpObservable(
        url: String,
        header: Map<String, String> = HashMap(),
        body: RequestBody? = null,
        useCookie: Boolean = true
    ): Observable<okhttp3.Response> {
        return Observable.create { emitter ->
            val httpCall = HttpUtil.getCall(url, header, body, useCookie)
            emitter.setCancellable {
                httpCall.cancel()
            }
            if (!emitter.isDisposed) httpCall.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (httpCall.isCanceled()) return
                    try {
                        emitter.onError(e)
                    } catch (inner: Throwable) {
                        Exceptions.throwIfFatal(inner)
                        RxJavaPlugins.onError(CompositeException(e, inner))
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (emitter.isDisposed) return
                    var terminated = false
                    try {
                        emitter.onNext(response)
                        if (!emitter.isDisposed) {
                            terminated = true
                            emitter.onComplete()
                        }
                    } catch (t: Throwable) {
                        Exceptions.throwIfFatal(t)
                        if (terminated) {
                            RxJavaPlugins.onError(t)
                        } else if (!emitter.isDisposed) {
                            try {
                                emitter.onError(t)
                            } catch (inner: Throwable) {
                                Exceptions.throwIfFatal(inner)
                                RxJavaPlugins.onError(CompositeException(t, inner))
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * Sax事件
     */
    enum class SaxEventType {
        NOTHING,
        BEGIN,
        END
    }

    /**
     * Sax解析
     * @param rsp Response
     * @param checkEvent Function2<XmlPullParser, String, SaxEventType>
     * @return String
     */
    fun parseWithSax(rsp: okhttp3.Response, checkEvent: (XmlPullParser, () -> String) -> SaxEventType): String {
        val parser = XmlPullParserFactory.newInstance().apply {
            this.isValidating = false
            this.setFeature(Xml.FEATURE_RELAXED, true)
            this.isNamespaceAware = false
        }.newPullParser()
        val stream = rsp.body!!.charStream()

        val charList = StringBuilder()
        var lastLineIndex = 0
        var lastClipIndex = 0
        val lineMap = arrayListOf(0, 0)

        parser.setInput(object : Reader() {
            override fun close() {
                stream.close()
            }

            override fun read(p0: CharArray, p1: Int, p2: Int): Int {
                val ret = stream.read(p0, p1, p2)
                if (ret > 0) {
                    charList.append(p0, p1, ret)
                    while (true) {
                        val nextLineIndex = charList.indexOf('\n', lastLineIndex - lastClipIndex + 1)
                        if (nextLineIndex < 0) break
                        lastLineIndex = nextLineIndex + lastClipIndex
                        lineMap.add(lastLineIndex)
                    }
                }
                return if (ret >= p1) ret - p1 else ret
            }
        })


        var lastEventLineNumber = 0
        var lastEventColumnNumber = 0
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = checkEvent(parser) {
                charList.substring(
                    0,
                    lineMap[parser.lineNumber] + parser.columnNumber - lastClipIndex
                )
            }
            if (event == SaxEventType.BEGIN) {
                val curIndex = lineMap[lastEventLineNumber] + lastEventColumnNumber
                charList.delete(0, curIndex - lastClipIndex)
                lastClipIndex = curIndex
            } else if (event == SaxEventType.END) break
            lastEventLineNumber = parser.lineNumber
            lastEventColumnNumber = parser.columnNumber
            try {
                parser.next()
            } catch (e: Exception) { /* no-op */
            }
        }
        return charList.toString()
    }
}