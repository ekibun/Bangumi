package soko.ekibun.bangumi.ui.subject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_subject.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.SwipeBackActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil

class SubjectActivity : SwipeBackActivity() {
    private val subjectPresenter: SubjectPresenter by lazy{ SubjectPresenter(this) }

    private val subject by lazy{ JsonUtil.toEntity(intent.getStringExtra(SubjectActivity.EXTRA_SUBJECT)?:"", Subject::class.java)?: {
        val id = Regex("""/subject/([0-9]+)""").find(intent.data!!.toString())!!.groupValues[1].toInt()
        Subject(id, "${Bangumi.SERVER}/subject/$id")
    }() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title=""

        subjectPresenter.init(subject)
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    var formhash = ""
    override fun onStart() {
        super.onStart()

        val cookieManager = CookieManager.getInstance()
        ApiHelper.buildHttpCall(Bangumi.SERVER, mapOf("User-Agent" to ua)){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            if(doc.selectFirst(".guest") != null) return@buildHttpCall null
            it.headers("set-cookie").forEach {
                cookieManager.setCookie(Bangumi.SERVER, it) }
            doc.selectFirst("input[name=formhash]")?.attr("value")
        }.enqueue(ApiHelper.buildCallback(this, { hash->
            if(hash.isNullOrEmpty()) return@buildCallback
            formhash = hash?:formhash
        }))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun processBack(){
        if(episode_detail_list.visibility == View.VISIBLE) return
        super.processBack()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            when {
                episode_detail_list.visibility == View.VISIBLE -> subjectPresenter.subjectView.closeEpisodeDetail()
                else -> finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_share -> AppUtil.shareString(this, subject.getPrettyName() + " " + subject.url)
            R.id.action_refresh -> subjectPresenter.refresh(subject)
        }
        return super.onOptionsItemSelected(item)
    }


    companion object{
        private const val EXTRA_SUBJECT = "extraSubject"

        fun startActivity(context: Context, subject: Subject) {
            context.startActivity(parseIntent(context, subject))
        }

        private fun parseIntent(context: Context, subject: Subject): Intent {
            val intent = Intent(context.applicationContext, SubjectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
