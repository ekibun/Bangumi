package soko.ekibun.bangumi.ui.say

import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oubowu.stickyitemdecoration.StickyHeadContainer
import com.oubowu.stickyitemdecoration.StickyItemDecoration
import kotlinx.android.synthetic.main.item_avatar_header.view.*
import kotlinx.android.synthetic.main.item_say.view.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.topic.PhotoPagerAdapter
import soko.ekibun.bangumi.ui.topic.PostAdapter
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.*
import java.lang.ref.WeakReference
import java.util.*

class SayAdapter(data: MutableList<SaySection>? = null) :
    BaseSectionQuickAdapter<SayAdapter.SaySection, BaseViewHolder>(R.layout.item_say, R.layout.item_say, data),
    FastScrollRecyclerView.SectionedAdapter {
    private var pinnedIndex = 0

    /**
     * 关联RecyclerView
     * @param recyclerView RecyclerView
     * @return DragSelectTouchListener
     */
    fun setUpWithRecyclerView(container: StickyHeadContainer, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        bindToRecyclerView(recyclerView)

        container.setDataCallback {
            val item = data[it]
            val isSelf = item.t.user.username == UserModel.current()?.username
            container.item_avatar_left.visibility = View.INVISIBLE
            container.item_avatar_right.visibility = View.INVISIBLE
            val avatar = if (isSelf) container.item_avatar_right else container.item_avatar_left
            avatar.visibility = View.VISIBLE

            recyclerView.layoutManager?.findViewByPosition(pinnedIndex)?.item_avatar?.visibility = View.VISIBLE
            recyclerView.layoutManager?.findViewByPosition(it)?.item_avatar?.visibility = View.INVISIBLE
            pinnedIndex = it
            container.visibility = View.VISIBLE
            avatar.setOnClickListener { v ->
                onItemChildClickListener?.onItemChildClick(this, v, it)
            }
            avatar.setOnLongClickListener { v ->
                onItemChildLongClickListener?.onItemChildLongClick(this, v, it) ?: false
            }
            updateAvatar(avatar, item)
        }

        recyclerView.addItemDecoration(StickyItemDecoration(container, SECTION_HEADER_VIEW))
    }

    private val imageSizes = HashMap<String, Size>()
    private val largeContent = WeakHashMap<String, Spanned>()
    override fun convert(helper: BaseViewHolder, item: SaySection) {
        helper.addOnClickListener(R.id.item_avatar)
        helper.addOnLongClickListener(R.id.item_avatar)
        helper.itemView.item_user.text = item.t.user.name

        if (data.indexOf(item) == 0) helper.setBackgroundRes(R.id.item_layout, R.drawable.bg_round_dialog)
        else helper.setBackgroundColor(
            R.id.item_layout,
            ResourceUtil.resolveColorAttr(helper.itemView.context, android.R.attr.colorBackground)
        )

        updateAvatar(helper.itemView.item_avatar, item)
        val isSelf = item.t.user.username == UserModel.current()?.username
        val showAvatar = item.isHeader
        helper.itemView.item_avatar.visibility =
            if (showAvatar && pinnedIndex != helper.layoutPosition) View.VISIBLE else View.INVISIBLE
        (helper.itemView.item_avatar.layoutParams as ConstraintLayout.LayoutParams).let {
            it.horizontalBias = if (isSelf) 1f else 0f
        }

        (helper.itemView.item_user.layoutParams as ConstraintLayout.LayoutParams).let {
            it.startToStart = if (isSelf) ConstraintLayout.LayoutParams.UNSET else helper.itemView.item_message.id
            it.endToEnd = if (isSelf) helper.itemView.item_message.id else ConstraintLayout.LayoutParams.UNSET
        }

        val dpStart = ResourceUtil.toPixels(helper.itemView.resources, 56f)
        val dpEnd = ResourceUtil.toPixels(helper.itemView.resources, 64f)
        (helper.itemView.item_message.layoutParams as ConstraintLayout.LayoutParams).let {
            it.horizontalBias = if (isSelf) 1f else 0f
            it.marginEnd = if (isSelf) dpStart else dpEnd
            it.marginStart = if (isSelf) dpEnd else dpStart
        }
        helper.itemView.item_message.setBackgroundResource(if (isSelf) R.drawable.bg_say_right else R.drawable.bg_say_left)

        helper.itemView.item_user.visibility = if (item.isHeader && !isSelf) View.VISIBLE else View.GONE

        val drawables = ArrayList<String>()
        helper.itemView.item_message.let { item_message ->
            val makeSpan = {
                @Suppress("DEPRECATION")
                TextUtil.setTextUrlCallback(
                    Html.fromHtml(
                        parseHtml(item.t.message),
                        HtmlHttpImageGetter(item_message, drawables, imageSizes) {
                            helper.itemView.width.toFloat() - dpStart - dpEnd
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
            item_message.text =
                TextUtil.updateTextViewRef(largeContent.getOrPut(item.t.message, makeSpan), WeakReference(item_message))
        }

        helper.itemView.item_message.onFocusChangeListener = View.OnFocusChangeListener { view, focus ->
            if (!focus) {
                view.tag = null
                (view as TextView).text = view.text
            }
        }
        helper.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()
        helper.itemView.item_message.requestLayout()
    }

    private fun updateAvatar(view: ImageView, item: SaySection) {
        GlideUtil.with(view)
            ?.load(Images.small(Bangumi.parseUrl(item.t.user.avatar ?: "")))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.into(view)
    }

    override fun getSectionName(position: Int): String {
        return "#$position"
    }

    /**
     * 对话项目（带section）
     * @constructor
     */
    class SaySection(isHeader: Boolean, reply: Say.SayReply) :
        SectionEntity<Say.SayReply>(isHeader, "") {
        init {
            t = reply
        }
    }

    override fun convertHead(helper: BaseViewHolder, item: SaySection) {
        convert(helper, item)
    }

    fun setNewData(say: Say) {
        super.setNewData(listOfNotNull(
            Say.SayReply(
                user = say.user,
                message = say.message ?: ""
            )
        ).plus(say.replies ?: ArrayList()).let {
            it.mapIndexed { index, sayReply ->
                SaySection(it.getOrNull(index - 1)?.user?.username != sayReply.user.username, sayReply)
            }
        })
    }

    companion object {
        /**
         * 转换Html
         * @param html String
         * @return String
         */
        fun parseHtml(html: String): String {
            val doc = Jsoup.parse(
                html
                    .replace("[img]<a ", "<aimg ").replace("</a>[/img]", "</aimg>")
                    .replace(Regex("""\[url=<a ([^>]*)>(.*?)<\/a>]"""), "<a $1>").replace("[/url]", "</a>")
                , Bangumi.SERVER
            )
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