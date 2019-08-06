package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import java.text.SimpleDateFormat
import java.util.*
import android.webkit.WebResourceResponse



class NestedWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.webViewStyle) : WebView(context, attrs, defStyleAttr) {

    var onProgressChanged = { view: WebView, newProgress: Int -> }
    var shouldOverrideUrlLoading = { view: WebView, request: WebResourceRequest -> false }
    var onReceivedTitle = { view: WebView?, title: String? -> }

    var parentWebView: NestedWebView? = null
    var childWebView: NestedWebView? = null

    init {
        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        /*
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.useWideViewPort = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        settings.loadWithOverviewMode = true

        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.allowFileAccessFromFileURLs = true
         */
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        webChromeClient = mWebChromeClient
        webViewClient = mWebViewClient
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN && scrollY <= 0)
            scrollTo(scrollX, 1)

        return super.onTouchEvent(event)
    }

    fun close(){
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

        val mWebChromeClient = object: WebChromeClient(){
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                val parent = (view.parent as? ViewGroup) ?: return false
                val webview = (view as? NestedWebView)?: return false
                val newView = NestedWebView(view.context)
                newView.onProgressChanged = webview.onProgressChanged
                //newView.shouldOverrideUrlLoading = webview.shouldOverrideUrlLoading
                newView.onReceivedTitle = webview.onReceivedTitle
                newView.parentWebView = webview
                webview.childWebView = newView
                newView.shouldOverrideUrlLoading = { _: WebView, request: WebResourceRequest ->
                    newView.loadUrl(request.url.toString())
                    newView.shouldOverrideUrlLoading = webview.shouldOverrideUrlLoading
                    true
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
                val webview = (window as? NestedWebView)?: return
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

        val mWebViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
                handler.proceed()
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return (view as? NestedWebView)?.shouldOverrideUrlLoading?.invoke(view, request) ?: super.shouldOverrideUrlLoading(view, request)
            }
        }
    }


}