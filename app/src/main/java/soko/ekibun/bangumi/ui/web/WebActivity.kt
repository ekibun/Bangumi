package soko.ekibun.bangumi.ui.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_web.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.util.AppUtil

class WebActivity : AppCompatActivity() {
    val api by lazy { Bangumi.createInstance(false) }

    private val isAuth by lazy{ intent.getBooleanExtra(IS_AUTH, false)}
    private val openUrl by lazy{ intent.getStringExtra(OPEN_URL)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val setProgress = { newProgress: Int ->
            webview_progress.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            webview_progress.progress = newProgress
        }

        webview_progress.max = 100
        @SuppressLint("SetJavaScriptEnabled")
        webview.settings.javaScriptEnabled = true
        if(!isAuth) {
            title = ""
            webview.loadUrl(openUrl)
            webview.webChromeClient = object:WebChromeClient(){
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (title != null) this@WebActivity.title = title
                }
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    setProgress(newProgress)
                }
            }
            webview.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    val id = Regex("""subject/([0-9]*)$""").find(url)?.groupValues?.get(1)?.toIntOrNull()
                    val openId = Regex("""subject/([0-9]*)$""").find(openUrl)?.groupValues?.get(1)?.toIntOrNull()
                    if(id != null && id != openId){
                        SubjectActivity.startActivity(this@WebActivity, Subject(id, url))
                        return true
                    }else if(!url.startsWith("http")){
                        try{
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, request.url), url))
                        }catch(e: Exception){
                            e.printStackTrace()
                            return false }
                        return true
                    }
                    return false
                }
            }
        }else{
            title = getString(R.string.login_auth)
            val authUrl = "${Bangumi.SERVER}/oauth/authorize?client_id=${Bangumi.APP_ID}&response_type=code"
            webview.webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    setProgress(newProgress)
                }
            }
            webview.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if(url == authUrl)
                        webview.loadUrl("$authUrl&redirect_uri=${Bangumi.REDIRECT_URL}")
                    else if(url.startsWith(Bangumi.REDIRECT_URL)){
                        webview.loadUrl("about:blank")
                        val uri = Uri.parse(url)
                        val code = uri.getQueryParameter("code")

                        val intent = Intent()
                        intent.putExtra(RESULT_CODE, code)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
            webview.loadUrl("$authUrl&redirect_uri=${Bangumi.REDIRECT_URL}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(!isAuth)
            menuInflater.inflate(R.menu.action_web, menu)
        return true
    }

    //back
    private fun processBack(){
        when {
            webview.canGoBack() -> webview.goBack()
            else -> {
                if(isAuth) setResult(Activity.RESULT_CANCELED, null)
                finish()
            }
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if(isAuth) setResult(Activity.RESULT_CANCELED, null)
                finish()
            }
            R.id.action_open -> AppUtil.openBrowser(this, webview.url)
            R.id.action_share -> AppUtil.shareString(this, webview.url)
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val REQUEST_AUTH = 1
        const val IS_AUTH = "auth"
        const val OPEN_URL = "openUrl"
        const val RESULT_CODE = "code"
        fun startActivityForAuth(context: Activity){
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(IS_AUTH, true)
            context.startActivityForResult(intent, REQUEST_AUTH)
        }

        fun launchUrl(context: Activity, page: String?){
            if(page.isNullOrEmpty()) return
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(OPEN_URL, page)
            context.startActivity(intent)
        }
    }
}
