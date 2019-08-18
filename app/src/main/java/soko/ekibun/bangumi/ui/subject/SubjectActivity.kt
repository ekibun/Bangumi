package soko.ekibun.bangumi.ui.subject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_subject.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.SwipeBackActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.PlayerBridge
import soko.ekibun.videoplayer.bean.VideoSubject

class SubjectActivity : SwipeBackActivity() {
    private val subjectPresenter: SubjectPresenter by lazy{ SubjectPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title=""

        subjectPresenter.init(
                if(intent.data?.toString()?.startsWith("ekibun://playersubject/bangumi") == true){
                    intent.getParcelableExtra<VideoSubject>(PlayerBridge.EXTRA_SUBJECT).toSubject()
                }else JsonUtil.toEntity(intent.getStringExtra(EXTRA_SUBJECT)?:"", Subject::class.java)?: {
                    val id = Regex("""/subject/([0-9]+)""").find(intent.data?.toString()?:"")?.groupValues?.get(1)?.toIntOrNull()?:0
                    Subject(id, "${Bangumi.SERVER}/subject/$id")
                }())

        val episodePaddingBottom = episode_detail_list.paddingBottom
        val listPaddingBottom = comment_list.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            episode_detail_list.setPadding(episode_detail_list.paddingLeft, episode_detail_list.paddingTop, episode_detail_list.paddingRight, episodePaddingBottom + insets.systemWindowInsetBottom)
            comment_list.setPadding(comment_list.paddingLeft, comment_list.paddingTop, comment_list.paddingRight, listPaddingBottom + insets.systemWindowInsetBottom)
            insets
        }
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    val formhash get() = subjectPresenter.subject.formhash?:""
    override fun onStart() {
        super.onStart()
        subjectPresenter.refresh()
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
            R.id.action_share -> AppUtil.shareString(this, subjectPresenter.subject.getPrettyName() + " " + subjectPresenter.subject.url)
            R.id.action_refresh -> subjectPresenter.refresh()
        }
        return super.onOptionsItemSelected(item)
    }


    companion object{
        private const val EXTRA_SUBJECT = "extraSubject"

        fun startActivity(context: Context, subject: Subject) {
            context.startActivity(parseIntent(context, subject))
        }

        private fun parseIntent(context: Context, subject: Subject): Intent {
            val intent = Intent(context, SubjectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
