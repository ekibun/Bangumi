package soko.ekibun.bangumi.ui.say

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_topic.*
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.TextUtil
import java.util.*

class SayActivity : BaseActivity(R.layout.activity_topic) {

    val sayPresenter by lazy {
        SayPresenter(
            this,
            JsonUtil.toEntity<Say>(intent.getStringExtra(EXTRA_SAY) ?: "")!!
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sayPresenter.say.let {
            App.get(this).historyModel.addHistory(
                HistoryModel.History(
                    type = "say",
                    title = TextUtil.html2text(it.message ?: ""),
                    subTitle = it.user.nickname,
                    thumb = it.user.avatar,
                    data = JsonUtil.toJson(
                        Say(
                            id = it.id,
                            user = it.user,
                            message = it.message,
                            time = it.time,
                            self = it.self
                        )
                    ),
                    timestamp = Calendar.getInstance().timeInMillis
                )
            )
        }

        val listPaddingBottom = item_list.paddingBottom
        val replyPaddingBottom = item_reply_container.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            item_list.setPadding(
                item_list.paddingLeft,
                item_list.paddingTop,
                item_list.paddingRight,
                listPaddingBottom + insets.systemWindowInsetBottom
            )
            item_reply_container.setPadding(
                item_reply_container.paddingLeft,
                item_reply_container.paddingTop,
                item_reply_container.paddingRight,
                replyPaddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ThemeModel.updateNavigationTheme(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> AppUtil.shareString(this, "${sayPresenter.say.message} ${sayPresenter.say.url}")
            R.id.action_refresh -> sayPresenter.getSay()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EXTRA_SAY = "extraSay"

        /**
         * 启动Activity
         * @param context Context
         * @param say Say
         */
        fun startActivity(context: Context, say: Say) {
            context.startActivity(parseIntent(context, say))
        }

        /**
         * intent
         * @param context Context
         * @param say Say
         * @return Intent
         */
        fun parseIntent(context: Context, say: Say): Intent {
            val intent = Intent(context, SayActivity::class.java)
            intent.putExtra(EXTRA_SAY, JsonUtil.toJson(say))
            return intent
        }
    }
}
