package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_timeline.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.TextUtil

/**
 * 时间线Adapter
 */
class TimeLineAdapter(data: MutableList<TimeLine>? = null) :
        BaseSectionQuickAdapter<TimeLine, BaseViewHolder>
        (R.layout.item_timeline, R.layout.header_episode, data) {
    override fun convertHead(helper: BaseViewHolder, item: TimeLine) {
        helper.setText(R.id.item_header, item.header)
    }

    override fun convert(helper: BaseViewHolder, item: TimeLine) {
        //say
        helper.itemView.item_layout.setOnClickListener {
            WebActivity.launchUrl(helper.itemView.context, Bangumi.parseUrl(item.t.sayUrl
                    ?: return@setOnClickListener), "")
        }
        helper.itemView.item_layout.isClickable = !item.t.sayUrl.isNullOrEmpty()
        //action
        @Suppress("DEPRECATION")
        helper.itemView.item_action.text = TextUtil.setTextUrlCallback(Html.fromHtml(item.t.action)) {
            WebActivity.launchUrl(helper.itemView.context, Bangumi.parseUrl(it), "")
        }
        helper.itemView.item_action.movementMethod = if (helper.itemView.item_layout.isClickable) null else LinkMovementMethod.getInstance()
        //del
        helper.itemView.item_del.visibility = if (item.t.delUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        helper.itemView.item_del.setOnClickListener {
            AlertDialog.Builder(helper.itemView.context).setMessage(R.string.timeline_dialog_remove)
                    .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                        ApiHelper.buildHttpCall("${item.t.delUrl}&ajax=1") {
                            it.body?.string()?.contains("\"status\":\"ok\"") == true
                        }.enqueue(ApiHelper.buildCallback<Boolean>({ success ->
                            if (!success) return@buildCallback
                            val index = data.indexOfFirst { it === item }
                            val removeHeader = data.getOrNull(index - 1)?.isHeader == true && data.getOrNull(index + 1)?.isHeader == true
                            remove(index)
                            if (removeHeader) remove(index - 1)
                        }) {})
                    }.show()
        }
        //collectStar
        helper.itemView.item_rate.visibility = if (item.t.collectStar == 0) View.GONE else View.VISIBLE
        helper.itemView.item_rate.rating = item.t.collectStar * 0.5f
        //content
        helper.itemView.item_content.visibility = if (item.t.content.isNullOrEmpty()) View.GONE else View.VISIBLE
        helper.itemView.item_content.text = item.t.content ?: ""
        helper.itemView.item_content.setOnClickListener {
            WebActivity.launchUrl(helper.itemView.context, item.t.contentUrl, "")
        }
        //time
        helper.itemView.item_time.text = item.t.time

        val userImage = item.t.user.avatar?.getImage(helper.itemView.context)
        helper.itemView.item_avatar.visibility = if (userImage.isNullOrEmpty() || data.getOrNull(data.indexOfFirst { it === item } - 1)?.t?.user?.username == item.t.user.username) View.INVISIBLE else View.VISIBLE
        helper.itemView.item_avatar.setOnClickListener {
            WebActivity.launchUrl(helper.itemView.context, item.t.user.url, "")
        }
        if (!userImage.isNullOrEmpty())
            GlideUtil.with(helper.itemView.item_avatar)
                    ?.load(userImage)
                    ?.apply(RequestOptions.circleCropTransform().error(R.drawable.err_404))
                    ?.into(helper.itemView.item_avatar)
    }

}