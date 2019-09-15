package soko.ekibun.bangumi.ui.topic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.SwipeBackActivity
import soko.ekibun.bangumi.util.AppUtil

class TopicActivity : SwipeBackActivity() {
    private val topicPresenter by lazy{ TopicPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        topicPresenter.getTopic(intent.getIntExtra(EXTRA_POST, 0).toString())

        val listPaddingBottom = item_list.paddingBottom
        val replyPaddingBottom = item_reply_container.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            item_list.setPadding(item_list.paddingLeft, item_list.paddingTop, item_list.paddingRight, listPaddingBottom + insets.systemWindowInsetBottom)
            item_reply_container.setPadding(item_reply_container.paddingLeft, item_reply_container.paddingTop, item_reply_container.paddingRight, replyPaddingBottom + insets.systemWindowInsetBottom)
            insets
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) ThemeModel.updateNavigationTheme(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_share -> AppUtil.shareString(this, "${title_expand.text} $openUrl")
            R.id.action_refresh -> topicPresenter.getTopic()
        }
        return super.onOptionsItemSelected(item)
    }

    val openUrl: String by lazy{ intent.getStringExtra(EXTRA_TOPIC) }

    class PostList: ArrayList<TopicPost>()

    companion object{
        private const val EXTRA_TOPIC = "extraTopic"
        private const val EXTRA_POST = "extraPost"

        fun startActivity(context: Context, topic: String, post: Int = 0) {
            context.startActivity(parseIntent(context, topic, post))
        }

        private fun parseIntent(context: Context, topic: String, post: Int): Intent {
            val intent = Intent(context, TopicActivity::class.java)
            intent.putExtra(EXTRA_TOPIC, topic)
            intent.putExtra(EXTRA_POST, post)
            return intent
        }
    }
}
