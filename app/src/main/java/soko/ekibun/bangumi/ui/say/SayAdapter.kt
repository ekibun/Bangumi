package soko.ekibun.bangumi.ui.say

import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_say.view.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.ui.topic.PhotoPagerAdapter
import soko.ekibun.bangumi.ui.topic.PostAdapter
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.*
import java.util.*

class SayAdapter(data: MutableList<Say.SayReply>? = null) :
    BaseQuickAdapter<Say.SayReply, BaseViewHolder>(R.layout.item_say, data), FastScrollRecyclerView.SectionedAdapter {
    var self: UserInfo? = null

    private val imageSizes = HashMap<String, Size>()
    private val largeContent = WeakHashMap<String, Spanned>()
    override fun convert(helper: BaseViewHolder, item: Say.SayReply) {
        helper.addOnClickListener(R.id.item_avatar)
        helper.addOnLongClickListener(R.id.item_avatar)
        helper.itemView.item_user.text = item.user.name
        val drawables = ArrayList<String>()
        helper.itemView.item_message.let { item_message ->
            val makeSpan = {
                @Suppress("DEPRECATION")
                TextUtil.setTextUrlCallback(
                    Html.fromHtml(
                        parseHtml(item.message),
                        HtmlHttpImageGetter(item_message, drawables, imageSizes) {
                            helper.itemView.width.toFloat() - ResourceUtil.toPixels(helper.itemView.resources, 50f)
                        },
                        HtmlTagHandler(item_message) { imageSpan ->
                            helper.itemView.item_message?.let { itemView ->
                                val imageList =
                                    drawables.filter { (it.startsWith("http") || !it.contains("smile")) }.toList()
                                val index = imageList.indexOfFirst { d -> d == imageSpan.source }
                                if (index < 0) return@HtmlTagHandler
                                PhotoPagerAdapter.showWindow(
                                    itemView,
                                    imageList.map { Bangumi.parseUrl(it) },
                                    index = index
                                )
                            }
                        })
                ) {
                    (helper.itemView.context as? TopicActivity)?.processUrl(Bangumi.parseUrl(it))
                        ?: WebActivity.launchUrl(helper.itemView.context, Bangumi.parseUrl(it), "")
                }
            }
            item_message.text = largeContent.getOrPut(item.message, makeSpan)
        }

        helper.itemView.item_message.onFocusChangeListener = View.OnFocusChangeListener { view, focus ->
            if (!focus) {
                view.tag = null
                (view as TextView).text = view.text
            }
        }
        helper.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()

        GlideUtil.with(helper.itemView.item_avatar)
            ?.load(Images.small(Bangumi.parseUrl(item.user.avatar ?: "")))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.into(helper.itemView.item_avatar)

        val isSelf = item.user.username == self?.username

        (helper.itemView.item_avatar.layoutParams as ConstraintLayout.LayoutParams).let {
            it.startToStart =
                if (isSelf) ConstraintLayout.LayoutParams.UNSET else ConstraintLayout.LayoutParams.PARENT_ID
            it.endToEnd = if (isSelf) ConstraintLayout.LayoutParams.PARENT_ID else ConstraintLayout.LayoutParams.UNSET
        }

        (helper.itemView.item_user.layoutParams as ConstraintLayout.LayoutParams).let {
            it.startToStart = if (isSelf) ConstraintLayout.LayoutParams.UNSET else helper.itemView.item_message.id
            it.endToEnd = if (isSelf) helper.itemView.item_message.id else ConstraintLayout.LayoutParams.UNSET
        }

        val dp8 = ResourceUtil.toPixels(helper.itemView.resources, 8f)
        val dp48 = ResourceUtil.toPixels(helper.itemView.resources, 48f)
        (helper.itemView.item_message.layoutParams as ConstraintLayout.LayoutParams).let {
            it.startToEnd = if (isSelf) ConstraintLayout.LayoutParams.UNSET else helper.itemView.item_avatar.id
            it.endToStart = if (isSelf) helper.itemView.item_avatar.id else ConstraintLayout.LayoutParams.UNSET
            it.endToEnd = if (isSelf) ConstraintLayout.LayoutParams.UNSET else ConstraintLayout.LayoutParams.PARENT_ID
            it.startToStart =
                if (isSelf) ConstraintLayout.LayoutParams.PARENT_ID else ConstraintLayout.LayoutParams.UNSET
            it.horizontalBias = if (isSelf) 1f else 0f
            it.marginEnd = if (isSelf) dp8 else dp48
            it.marginStart = if (isSelf) dp48 else dp8
        }
        helper.itemView.item_message.setBackgroundResource(if (isSelf) R.drawable.bg_say_right else R.drawable.bg_say_left)

        val showAvatar = data.getOrNull(helper.adapterPosition - 1)?.user?.username != item.user.username
        helper.itemView.item_avatar.visibility = if (showAvatar) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_user.visibility = if (showAvatar) View.VISIBLE else View.GONE

        helper.itemView.item_message.requestLayout()
        helper.itemView.requestLayout()
    }

    override fun getSectionName(position: Int): String {
        return "#$position"
    }


    companion object {
        /**
         * 转换Html
         * @param html String
         * @return String
         */
        fun parseHtml(html: String): String {
            val doc = Jsoup.parse(html.replace("[img]<a ", "<aimg ").replace("</a>[/img]", "</aimg>"), Bangumi.SERVER)
            doc.outputSettings().indentAmount(0).prettyPrint(false)
            doc.select("aimg").map {
                it.html("[img]${it.attr("href")}[/img]")
            }
            @Suppress("DEPRECATION")
            return PostAdapter.parseHtml(
                TextUtil.bbcode2html(
                    TextUtil.span2bbcode(
                        Html.fromHtml(
                            doc.body().html().trim()
                        )
                    )
                )
            )
        }
    }
}