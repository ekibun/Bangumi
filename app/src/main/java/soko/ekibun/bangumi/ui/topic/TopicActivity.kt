package soko.ekibun.bangumi.ui.topic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_topic.*
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 帖子Activity
 * @property topicPresenter TopicPresenter
 */
class TopicActivity : BaseActivity(R.layout.activity_topic) {
    val topicPresenter by lazy { TopicPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        topicPresenter.init(
            JsonUtil.toEntity<Topic>(intent.getStringExtra(EXTRA_TOPIC) ?: "")!!,
            intent.getIntExtra(EXTRA_POST, 0).toString()
        )

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
            R.id.action_share -> AppUtil.shareString(this, "${topicPresenter.topic.title} ${topicPresenter.topic.url}")
            R.id.action_refresh -> topicPresenter.getTopic()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 处理url
     * @param url String
     */
    fun processUrl(url: String) {
        WebActivity.parseUrlIntent(this, url, "")?.let {
            if (it.hasExtra(EXTRA_TOPIC) && JsonUtil.toEntity<Topic>(
                    it.getStringExtra(EXTRA_TOPIC) ?: ""
                )?.id == topicPresenter.topic.id
            ) {
                topicPresenter.topicView.scrollToPost(it.getIntExtra(EXTRA_POST, 0).toString(), true)
            } else startActivity(it)
        }
    }

    companion object {
        private const val EXTRA_TOPIC = "extraTopic"
        private const val EXTRA_POST = "extraPost"

        /**
         * 启动Activity
         * @param context Context
         * @param topic Topic
         * @param post Int
         */
        fun startActivity(context: Context, topic: Topic, post: Int = 0) {
            context.startActivity(parseIntent(context, topic, post))
        }

        /**
         * intent
         * @param context Context
         * @param topic Topic
         * @param post Int
         * @return Intent
         */
        fun parseIntent(context: Context, topic: Topic, post: Int): Intent {
            val intent = Intent(context, TopicActivity::class.java)
            intent.putExtra(EXTRA_TOPIC, JsonUtil.toJson(topic))
            intent.putExtra(EXTRA_POST, post)
            return intent
        }
    }
}
