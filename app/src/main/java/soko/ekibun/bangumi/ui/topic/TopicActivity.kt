package soko.ekibun.bangumi.ui.topic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import android.view.*
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

        topicPresenter.getTopic(intent.getIntExtra(TopicActivity.EXTRA_POST, 0).toString())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_TOPIC, topic)
            intent.putExtra(EXTRA_POST, post)
            return intent
        }
    }
}
