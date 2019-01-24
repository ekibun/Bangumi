package soko.ekibun.bangumi.ui.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import kotlinx.android.synthetic.main.activity_web.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.util.AppUtil
import java.net.URI

class WebActivity : AppCompatActivity() {
    val api by lazy { Bangumi.createInstance(false) }

    private val isAuth by lazy{ intent.getBooleanExtra(IS_AUTH, false)}
    private val openUrl by lazy{ intent.getStringExtra(OPEN_URL)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val setProgress = { newProgress: Int ->
            webview_progress.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            item_swipe.isRefreshing = newProgress != 100
            webview_progress.progress = newProgress
        }

        item_swipe.setOnRefreshListener {
            webview.reload()
        }

        webview_progress.max = 100
        @SuppressLint("SetJavaScriptEnabled")
        webview.settings.javaScriptEnabled = true
        webview.settings.domStorageEnabled = true
        webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                    if(jumpUrl(this@WebActivity, url, openUrl)){
                        return true
                    }else if(!url.startsWith("http") || !isBgmPage(url)){
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
                    if(url.startsWith(Bangumi.REDIRECT_URL)){
                        webview.loadUrl("about:blank")
                        val uri = Uri.parse(url)
                        val code = uri.getQueryParameter("code")

                        val intent = Intent()
                        intent.putExtra(RESULT_CODE, code)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }else if(url == authUrl || !url.startsWith(authUrl) && !url.startsWith("${Bangumi.SERVER}/login") && !url.startsWith("${Bangumi.SERVER}/FollowTheRabbit")){
                        Log.v("redirect", url)
                        webview.loadUrl("$authUrl&redirect_uri=${Bangumi.REDIRECT_URL}")
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

        fun launchUrl(context: Context, page: String?){
            if(page.isNullOrEmpty()) return
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(OPEN_URL, page)
            context.startActivity(intent)
        }

        fun launchUrl(context: Context, url: String?, openUrl: String){
            if(jumpUrl(context, url, openUrl)) return
            if(url?.startsWith("http") == false || !isBgmPage(url?:"")){
                try{
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url)), url))
                    return
                }catch(e: Exception){
                    e.printStackTrace()
                }
            }
            launchUrl(context, url)
        }

        fun isBgmPage(url: String): Boolean{
            val host = try{
                URI.create(url).host
            }catch (e: Exception){ return false }
            bgmHosts.forEach {
                if(host.contains(it)) return true
            }
            return false
        }

        private val bgmHosts = arrayOf("bgm.tv", "bangumi.tv", "chii.in")
        fun jumpUrl(context: Context, page: String?, openUrl: String): Boolean{
            val url = page?.split("#")?.get(0)
            if(url == null || url.isNullOrEmpty() || url == openUrl) return false
            if(!isBgmPage(url)) return false
            val post = Regex("""#post_([0-9]+)$""").find(page)?.groupValues?.get(1)?.toIntOrNull()?:0
            //Topic
            var regex = Regex("""/m/topic/[^/]*/([0-9]*)$""")
            var id = regex.find(url)?.groupValues?.get(1)?.toIntOrNull()
            if(id != null){
                TopicActivity.startActivity(context, url, post)
                return true }
            regex = Regex("""/rakuen/topic/([^/]*)/([0-9]*)$""")
            var model = regex.find(url)?.groupValues?.get(1)?:""
            id = regex.find(url)?.groupValues?.get(2)?.toIntOrNull()
            if(id != null){
                TopicActivity.startActivity(context, "${Bangumi.SERVER}/m/topic/$model/$id", post)
                return true }
            regex = Regex("""/([^/]*)/topic/([0-9]*)$""")
            model = regex.find(url)?.groupValues?.get(1)?:""
            id = regex.find(url)?.groupValues?.get(2)?.toIntOrNull()
            if(id != null){
                TopicActivity.startActivity(context, "${Bangumi.SERVER}/m/topic/$model/$id", post)
                return true }
            //Subject
            regex = Regex("""/subject/([0-9]*)$""")
            id = regex.find(url)?.groupValues?.get(1)?.toIntOrNull()
            if(id != null){
                SubjectActivity.startActivity(context, Subject(id, url))
                return true }
            return false
        }
    }
}
