package soko.ekibun.bangumi.ui.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_auth.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi

class AuthActivity : AppCompatActivity() {
    val api by lazy { Bangumi.createInstance(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val authUrl = "${Bangumi.SERVER}/oauth/authorize?client_id=${Bangumi.APP_ID}&response_type=code"
        webview.settings.javaScriptEnabled = true
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
        webview_progress.max = 100
        webview.webChromeClient = object: WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                webview_progress.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
                webview_progress.progress = newProgress
            }
        }
        webview.loadUrl("$authUrl&redirect_uri=${Bangumi.REDIRECT_URL}")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED, null)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val REQUEST_AUTH = 1
        const val RESULT_CODE = "code"
        fun startActivityForResult(context: Activity){
            val intent = Intent(context, AuthActivity::class.java)
            context.startActivityForResult(intent, REQUEST_AUTH)
        }
    }
}
