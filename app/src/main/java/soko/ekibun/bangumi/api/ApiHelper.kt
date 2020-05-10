package soko.ekibun.bangumi.api

import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.Exceptions
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import okhttp3.RequestBody
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

/**
 * API工具库
 */
object ApiHelper {
    class DisposeContainer {
        private val disposables = CompositeDisposable()
        private val keyDisposable = HashMap<String, Disposable>()

        /**
         * 在主线程回调
         * - `onError`中调用`onComplete`保持协同
         * - `onError`默认弹出[Toast]:
         *    ```
         *    Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
         *    ```
         */
        fun <T> subscribeOnUiThread(
            observable: Observable<T>, onNext: (t: T) -> Unit,
            onError: (t: Throwable) -> Unit = {
                it.printStackTrace()
            },
            onComplete: () -> Unit = {},
            key: String? = null
        ): Disposable {
            if (!key.isNullOrEmpty()) keyDisposable[key]?.dispose()
            return observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, {
                    if (!it.toString().toLowerCase(Locale.ROOT).contains("canceled")) {
                        Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
                        it.printStackTrace()
                        onError(it)
                    }
                    onComplete()
                }, onComplete).also {
                    if (!key.isNullOrEmpty()) keyDisposable[key] = it
                    disposables.add(it)
                }
        }

        fun dispose() {
            disposables.dispose()
        }

        fun dispose(key: String) {
            keyDisposable.remove(key)?.dispose()
        }
    }

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

    private val tagMatcher = "<([a-z]+?)(.+?)>".toRegex(RegexOption.IGNORE_CASE)
    private val attrMatcher = """ (.*?)="(.*?)"""".toRegex()

    /**
     * Sax解析
     * @param rsp Response
     * @param checkEvent Function2<Element, String, SaxEventType>
     * @return String
     */
    fun parseSax(rsp: okhttp3.Response, checkEvent: (Element, () -> String) -> SaxEventType): String {
        val stream = rsp.body!!.charStream()
        val chars = StringBuilder()
        val buffer = CharArray(8192)
        var lastLineIndex = 0
        var lastClipIndex = 0
        outer@ while (true) {
            val len = stream.read(buffer)
            if (len < 0) break
            chars.append(buffer, 0, len)
            val findLastClipIndex = lastClipIndex
            for (match in tagMatcher.findAll(chars, lastLineIndex - lastClipIndex)) {
                lastLineIndex = match.range.last + findLastClipIndex + 1
                val curIndex = match.range.first + findLastClipIndex
                val event =
                    checkEvent(Element(Tag.valueOf(match.groupValues[1]), Bangumi.SERVER, Attributes().also { attrs ->
                        attrMatcher.findAll(match.groupValues[2]).forEach {
                            attrs.add(it.groupValues[1], it.groupValues[2])
                        }
                    })) {
                        chars.substring(lastClipIndex - findLastClipIndex, curIndex - findLastClipIndex)
                    }
                if (event == SaxEventType.BEGIN) {
                    lastClipIndex = curIndex
                } else if (event == SaxEventType.END) break@outer
            }
            chars.delete(0, lastClipIndex - findLastClipIndex)
        }
        return chars.toString()
    }
}