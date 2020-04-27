package soko.ekibun.bangumi.ui.topic

import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.View
import android.widget.TextView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.BaseNodeAdapter
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 回复 Adapter
 * @constructor
 */
class PostAdapter() :
    BaseNodeAdapter(), FastScrollRecyclerView.SectionedAdapter, LoadMoreModule {

    init {
        addFullSpanNodeProvider(NodeProvider())
    }

    override fun getItemType(data: List<BaseNode>, position: Int): Int {
        return if (data.get(position) is TopicPost) 0 else -1
    }

    override fun getSectionName(position: Int): String {
        val item = (data.getOrNull(position) ?: data.lastOrNull()) as? TopicPost
        return "#${item?.floor}"
    }

    class NodeProvider : BaseNodeProvider() {
        override val itemViewType: Int = 0
        override val layoutId: Int = R.layout.item_reply

        private val imageSizes = HashMap<String, Size>()
        private val largeContent = WeakHashMap<String, Spanned>()

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            (getAdapter() as PostAdapter).let {
                it.addOnClickListener(helper, R.id.item_del)
                it.addOnClickListener(helper, R.id.item_reply)
                it.addOnClickListener(helper, R.id.item_edit)
                it.addOnClickListener(helper, R.id.item_avatar)
            }
            if (item !is TopicPost) return
            // 第一项是圆角布局
            if (helper.adapterPosition == 0) helper.setBackgroundResource(R.id.item_layout, R.drawable.bg_round_dialog)
            else helper.setBackgroundColor(
                R.id.item_layout,
                ResourceUtil.resolveColorAttr(helper.itemView.context, android.R.attr.colorBackground)
            )
            // 用户
            helper.itemView.item_user.text =
                if (item.badge.isNullOrEmpty()) item.nickname else "${item.nickname} ${item.pst_content}"
            helper.itemView.item_user_sign.text =
                if (item.badge.isNullOrEmpty()) if (item.sign.length < 2) "" else item.sign.substring(
                    1,
                    item.sign.length - 1
                ) else item.dateline
            val subFloor = if (item.sub_floor > 0) "-${item.sub_floor}" else ""
            helper.itemView.item_time.text = (if (item.floor > 0) "#${item.floor}$subFloor - " else "") + item.dateline
            helper.itemView.item_reply.visibility = if (item.relate.toIntOrNull() ?: 0 > 0) View.VISIBLE else View.GONE
            helper.itemView.item_del.visibility = if (item.editable) View.VISIBLE else View.GONE
            helper.itemView.item_edit.visibility = helper.itemView.item_del.visibility

            helper.itemView.item_expand.visibility = if (item.children.size > 0) View.VISIBLE else View.GONE
            helper.itemView.item_expand.setText(if (item.isExpanded) R.string.collapse else R.string.expand)
            helper.itemView.item_expand.setOnClickListener {
                getAdapter()?.expandOrCollapse(helper.layoutPosition)
            }

            GlideUtil.with(helper.itemView.item_avatar)
                ?.load(Images.small(Bangumi.parseUrl(item.avatar)))
                ?.apply(
                    RequestOptions.circleCropTransform().error(R.drawable.err_404)
                        .placeholder(R.drawable.placeholder_round)
                )
                ?.into(helper.itemView.item_avatar)

            helper.itemView.item_message.visibility = if (item.badge.isNullOrEmpty()) View.VISIBLE else View.GONE
            helper.itemView.item_action_box.visibility = helper.itemView.item_message.visibility
            helper.itemView.offset.text = item.badge
            helper.itemView.offset.visibility = when {
                !item.badge.isNullOrEmpty() -> View.VISIBLE
                item.isSub -> View.INVISIBLE
                else -> View.GONE
            }

            if (item.badge.isNullOrEmpty()) {
                val drawables = ArrayList<String>()
                helper.itemView.item_message.let { item_message ->
                    val makeSpan = {
                        @Suppress("DEPRECATION")
                        TextUtil.setTextUrlCallback(
                            Html.fromHtml(
                                parseHtml(item.pst_content),
                                HtmlHttpImageGetter(item_message, drawables, imageSizes, {
                                    item_message.width.toFloat()
                                }),
                                HtmlTagHandler(item_message) { imageSpan ->
                                    helper.itemView.item_message?.let { itemView ->
                                        val imageList =
                                            drawables.filter { (it.startsWith("http") || !it.contains("smile")) }
                                                .toList()
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
                    item_message.text = TextUtil.updateTextViewRef(
                        largeContent.getOrPut(item.pst_content, makeSpan),
                        WeakReference(item_message)
                    )
                }
                helper.itemView.item_message.onFocusChangeListener = View.OnFocusChangeListener { view, focus ->
                    if (!focus) {
                        view.tag = null
                        (view as TextView).text = view.text
                    }
                }
                helper.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }


    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)

        // Bug workaround for losing text selection ability, see:
        // https://code.google.com/p/android/issues/detail?id=208169
        holder.itemView.item_message?.isEnabled = false
        holder.itemView.item_message?.isEnabled = true
    }

    companion object {
        /**
         * 转换Html
         * @param html String
         * @return String
         */
        fun parseHtml(html: String): String {
            val doc = Jsoup.parse(html.replace(Regex("</?noscript>"), ""), Bangumi.SERVER)
            doc.outputSettings().indentAmount(0).prettyPrint(false)
            doc.select("script").remove()
            doc.select("img").forEach {
                if (!it.hasAttr("src")) it.remove()
            }
            doc.body().children().forEach {
                var appendBefore = ""
                var appendEnd = ""
                val style = it.attr("style")
                if (style.contains("font-weight:bold")) {
                    appendBefore = "$appendBefore<b>"
                    appendEnd = "</b>$appendEnd"
                } //it.html("<b>${parseHtml(it.html())}</b>")
                if (style.contains("font-style:italic")) {
                    appendBefore = "$appendBefore<i>"
                    appendEnd = "</i>$appendEnd"
                } //it.html("<i>${parseHtml(it.html())}</i>")
                if (style.contains("text-decoration: underline")) {
                    appendBefore = "$appendBefore<u>"
                    appendEnd = "</u>$appendEnd"
                } //it.html("<u>${parseHtml(it.html())}</u>")
                if (style.contains("font-size:")) {
                    Regex("""font-size:([0-9]*)px""").find(style)?.groupValues?.get(1)?.let { size ->
                        appendBefore = "$appendBefore<size size='${size}px'>"
                        appendEnd = "</size>$appendEnd"
                    }
                }//it.html("<size size='${size}px'>${parseHtml(it.html())}</size>")
                if (style.contains("background-color:")) {
                    appendBefore = "$appendBefore<mask>"
                    appendEnd = "</mask>$appendEnd"
                } //it.html("<mask>${parseHtml(it.html())}</mask>")
                it.html("$appendBefore${parseHtml(it.html())}$appendEnd")
            }
            doc.select("div.quote").forEach {
                it.html("“${it.html()}”")
            }
            return doc.body().html()
        }
    }
}