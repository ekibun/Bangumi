package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Message
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 多窗口WebView
 * @property onProgressChanged Function2<WebView, Int, Unit>
 * @property shouldOverrideUrlLoading Function2<WebView, WebResourceRequest, Boolean>
 * @property onReceivedTitle Function2<WebView?, String?, Unit>
 * @property onShowFileChooser Function2<ValueCallback<Array<Uri>>?, FileChooserParams?, Boolean>
 * @property parentWebView NestedWebView?
 * @property childWebView NestedWebView?
 * @constructor
 */
class NestedWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {

    var onProgressChanged = { _: WebView, _: Int -> }
    var shouldOverrideUrlLoading = { _: WebView, _: String -> false }
    var onReceivedTitle = { _: WebView?, _: String? -> }

    var onShowFileChooser = { _: ValueCallback<Array<Uri>>?, _: WebChromeClient.FileChooserParams? -> false }

    var parentWebView: NestedWebView? = null
    var childWebView: NestedWebView? = null

    init {
        settings.userAgentString = HttpUtil.ua

        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        /*
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.allowFileAccessFromFileURLs = true
         */
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        webChromeClient = mWebChromeClient
        webViewClient = mWebViewClient
    }

    var overScrollY = false
    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        overScrollY = clampedY
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) overScrollY = false
        return super.onTouchEvent(event)
    }

    /**
     * 关闭窗口
     */
    fun close() {
        val parentWebView = parentWebView ?: return
        val parent = (parent as? ViewGroup) ?: return
        parent.removeView(this)
        parent.addView(parentWebView, layoutParams)
        parentWebView.childWebView = null
        parentWebView.onReceivedTitle(parentWebView, parentWebView.title)

        removeAllViews()
        destroy()
    }

    companion object {

        val mWebChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return (webView as? NestedWebView)?.onShowFileChooser?.invoke(filePathCallback, fileChooserParams)
                    ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val parent = (view.parent as? ViewGroup) ?: return false
                val webview = (view as? NestedWebView) ?: return false
                val newView = NestedWebView(view.context)
                newView.onProgressChanged = webview.onProgressChanged
                newView.onShowFileChooser = webview.onShowFileChooser
                newView.onReceivedTitle = webview.onReceivedTitle
                newView.parentWebView = webview
                webview.childWebView = newView
                newView.shouldOverrideUrlLoading = { _, req: String ->
                    newView.shouldOverrideUrlLoading = webview.shouldOverrideUrlLoading
                    if (newView.shouldOverrideUrlLoading(newView, req)) {
                        newView.close()
                        true
                    } else false
                }
                val layoutParams = webview.layoutParams
                parent.removeView(webview)
                parent.addView(newView, 0, layoutParams)

                val transport = resultMsg.obj as WebViewTransport
                transport.webView = newView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                val webview = (window as? NestedWebView) ?: return
                webview.close()
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                (view as? NestedWebView)?.onReceivedTitle?.invoke(view, title)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                (view as? NestedWebView)?.onProgressChanged?.invoke(view, newProgress)
            }
        }

        fun updateViewPort(view: WebView) {
            if (!PreferenceManager.getDefaultSharedPreferences(view.context)
                    .getBoolean("webview_fix_scale", true)
            ) return
            view.evaluateJavascript(
                """
                    (function(){
                        document.getElementsByName('viewport').forEach((v)=>{ v.parentNode.removeChild(v) })
                        document.head.innerHTML += '<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">'
                    })()
                """.trimIndent()
            ) {}
        }

        var useDeprecatedMethod = true
        val mWebViewClient = object : WebViewClient() {

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                return (useDeprecatedMethod && ((view as? NestedWebView)?.shouldOverrideUrlLoading?.invoke(view, url)
                    ?: false)) || super.shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                useDeprecatedMethod = false
                return ((view as? NestedWebView)?.shouldOverrideUrlLoading?.invoke(view, request.url.toString())
                    ?: false) || super.shouldOverrideUrlLoading(view, request)
            }

            override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                super.onScaleChanged(view, oldScale, newScale)
                updateViewPort(view)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                updateViewPort(view)
            }
        }
    }


}