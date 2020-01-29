package soko.ekibun.bangumi.ui.subject

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_subject.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.SwipeBackActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.PlayerBridge
import soko.ekibun.videoplayer.bean.VideoSubject

/**
 * 条目Activity
 */
class SubjectActivity : SwipeBackActivity() {
    private val subjectPresenter: SubjectPresenter by lazy { SubjectPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        subjectPresenter.init(
            if (intent.data?.toString()?.startsWith("ekibun://playersubject/bangumi") == true) {
                intent.getParcelableExtra<VideoSubject>(PlayerBridge.EXTRA_SUBJECT)!!.toSubject()
            } else JsonUtil.toEntity<Subject>(intent.getStringExtra(EXTRA_SUBJECT) ?: "") ?: {
                val id = Regex("""/subject/([0-9]+)""").find(
                    intent.data?.toString()
                        ?: ""
                )?.groupValues?.get(1)?.toIntOrNull() ?: 0
                Subject(id)
            }()
        )
        subjectPresenter.updateConfiguration()

        val episodePaddingBottom = episode_detail_list.paddingBottom
        val listPaddingBottom = comment_list.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            episode_detail_list.setPadding(episode_detail_list.paddingLeft, episode_detail_list.paddingTop, episode_detail_list.paddingRight, episodePaddingBottom + insets.systemWindowInsetBottom)
            comment_list.setPadding(comment_list.paddingLeft, comment_list.paddingTop, comment_list.paddingRight, listPaddingBottom + insets.systemWindowInsetBottom)
            toolbar_container.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ThemeModel.updateNavigationTheme(this)
    }

    override fun onStart() {
        subjectPresenter.refresh()
        super.onStart()
    }

    override fun processBack() {
        if (episode_detail_list_container.visibility == View.VISIBLE) return
        super.processBack()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            when (episode_detail_list_container.visibility) {
                View.VISIBLE -> subjectPresenter.subjectView.closeEpisodeDetail()
                else -> finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_share -> AppUtil.shareString(
                this,
                subjectPresenter.subject.displayName + " " + subjectPresenter.subject.url
            )
            R.id.action_refresh -> subjectPresenter.refresh()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        subjectPresenter.updateConfiguration()
    }

    companion object {
        private const val EXTRA_SUBJECT = "extraSubject"
        /**
         * 启动Activity
         */
        fun startActivity(context: Context, subject: Subject) {
            context.startActivity(parseIntent(context, subject))
        }

        private fun parseIntent(context: Context, subject: Subject): Intent {
            val intent = Intent(context, SubjectActivity::class.java)
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
