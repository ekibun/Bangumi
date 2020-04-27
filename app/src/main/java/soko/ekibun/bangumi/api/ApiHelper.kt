package soko.ekibun.bangumi.api

import android.util.Xml
import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.RequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException
import java.io.Reader
import java.util.*
import kotlin.collections.HashMap

/**
 * API工具库
 */
object ApiHelper {
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
            it.printStackTrace()
        },
        onComplete: () -> Unit = {},
        key: String? = null
    ): Disposable {
        if (key != null) observables.remove(key)?.dispose()
        return this.observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, {
                if (!it.toString().toLowerCase(Locale.ROOT).contains("canceled")) {
                    Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
                    onError(it)
                }
                onComplete()
            }, onComplete).also {
                if (key != null) observables[key] = it
            }
    }

    private val observables = HashMap<String, Disposable>()

    /**
     * 创建OkHttp的[Observable]
     * - 运行在[Schedulers.computation]
     */
    fun createHttpObservable(
        url: String,
        header: Map<String, String> = HashMap(),
        body: RequestBody? = null,
        useCookie: Boolean = true
    ): Observable<okhttp3.Response> {
        return Observable.create<okhttp3.Response> { emitter ->
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
        }.subscribeOn(Schedulers.computation())
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