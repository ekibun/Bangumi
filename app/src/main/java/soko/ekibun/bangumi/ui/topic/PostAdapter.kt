package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.ui.view.FixMultiViewPager
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HtmlHttpImageGetter
import soko.ekibun.bangumi.util.HtmlTagHandler
import soko.ekibun.bangumi.util.TextUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 回复 Adapter
 */
class PostAdapter(data: MutableList<TopicPost>? = null) :
        BaseMultiItemQuickAdapter<TopicPost, BaseViewHolder>(data), FastScrollRecyclerView.SectionedAdapter {

    init {
        addItemType(0, R.layout.item_reply)
        addItemType(1, R.layout.item_reply)
    }

    override fun getSectionName(position: Int): String {
        val item = data.getOrNull(position) ?: data.last()
        return "#${item.floor}"
    }

    private val imaageSizes = HashMap<String, Size>()
    private val largeContent = WeakHashMap<String, Spanned>()
    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: TopicPost) {
        helper.addOnClickListener(R.id.item_del)
        helper.addOnClickListener(R.id.item_reply)
        helper.addOnClickListener(R.id.item_edit)
        helper.addOnClickListener(R.id.item_avatar)
        helper.itemView.item_user.text = if (item.badge.isNullOrEmpty()) item.nickname else "${item.nickname} ${item.pst_content}"
        helper.itemView.item_user_sign.text = if (item.badge.isNullOrEmpty()) if (item.sign.length < 2) "" else item.sign.substring(1, item.sign.length - 1) else item.dateline
        val subFloor = if (item.sub_floor > 0) "-${item.sub_floor}" else ""
        helper.itemView.item_time.text = "#${item.floor}$subFloor - ${item.dateline}"
        helper.itemView.item_reply.visibility = if (item.relate.toIntOrNull() ?: 0 > 0) View.VISIBLE else View.GONE
        helper.itemView.item_del.visibility = if (item.editable) View.VISIBLE else View.GONE
        helper.itemView.item_edit.visibility = helper.itemView.item_del.visibility

        helper.itemView.item_expand.visibility = if (item.hasSubItem()) View.VISIBLE else View.GONE
        helper.itemView.item_expand.setText(if (item.isExpanded) R.string.collapse else R.string.expand)
        helper.itemView.item_expand.setOnClickListener {
            val index = data.indexOfFirst { post -> post === item }
            if (item.isExpanded) collapse(index) else expand(index)
        }

        GlideUtil.with(helper.itemView.item_avatar)
                ?.load(Images(Bangumi.parseUrl(item.avatar)).small)
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.apply(RequestOptions.circleCropTransform())
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
                    TextUtil.setTextUrlCallback(Html.fromHtml(parseHtml(item.pst_content), HtmlHttpImageGetter(item_message, drawables, imaageSizes), HtmlTagHandler(item_message) { imageSpan ->
                        helper.itemView.item_message?.let { itemView ->
                            val imageList = drawables.filter { (it.startsWith("http") || !it.contains("smile")) }.toList()
                            val index = imageList.indexOfFirst { d -> d == imageSpan.source }
                            if (index < 0) return@HtmlTagHandler
                            val popWindow = PopupWindow(itemView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
                            val viewPager = FixMultiViewPager(itemView.context)
                            popWindow.contentView = viewPager
                            viewPager.adapter = PhotoPagerAdapter(imageList.map { Bangumi.parseUrl(it) }) {
                                popWindow.dismiss()
                            }
                            viewPager.currentItem = index
                            popWindow.isClippingEnabled = false
                            popWindow.animationStyle = R.style.AppTheme_FadeInOut
                            popWindow.showAtLocation(itemView, Gravity.CENTER, 0, 0)
                            popWindow.contentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
                        }
                    })) {
                        WebActivity.launchUrl(helper.itemView.context, Bangumi.parseUrl(it), "")
                    }
                }
                item_message.text = if (item.pst_content.length < 10000) makeSpan() else largeContent.getOrPut(item.pst_id, makeSpan)
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