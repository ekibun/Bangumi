package soko.ekibun.bangumi.ui.subject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 条目Activity
 * @property subjectPresenter SubjectPresenter
 */
class SubjectActivity : BaseActivity(R.layout.activity_subject) {
    private val subjectPresenter: SubjectPresenter by lazy {
        SubjectPresenter(this, JsonUtil.toEntity<Subject>(intent.getStringExtra(EXTRA_SUBJECT) ?: "") ?: {
            val id = Regex("""/subject/([0-9]+)""").find(
                intent.data?.toString()
                    ?: ""
            )?.groupValues?.get(1)?.toIntOrNull() ?: 0
            Subject(id)
        }())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subjectPresenter.updateHistory()
        subjectPresenter.refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ThemeModel.updateNavigationTheme(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> AppUtil.shareString(
                this,
                subjectPresenter.subject.displayName + " " + subjectPresenter.subject.url
            )
            R.id.action_refresh -> subjectPresenter.refresh()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EXTRA_SUBJECT = "extraSubject"
        /**
         * 启动Activity
         * @param context Context
         * @param subject Subject
         */
        fun startActivity(context: Context, subject: Subject) {
            context.startActivity(parseIntent(context, subject))
        }

        /**
         * intent
         * @param context Context
         * @param subject Subject
         * @return Intent
         */
        fun parseIntent(context: Context, subject: Subject): Intent {
            val intent = Intent(context, SubjectActivity::class.java)
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
