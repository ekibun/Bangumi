package soko.ekibun.bangumi.ui.say

import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.entity.SectionEntity
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.oubowu.stickyitemdecoration.StickyHeadContainer
import com.oubowu.stickyitemdecoration.StickyItemDecoration
import kotlinx.android.synthetic.main.item_avatar_header.view.*
import kotlinx.android.synthetic.main.item_say.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HtmlUtil
import soko.ekibun.bangumi.util.ResourceUtil
import java.util.*

class SayAdapter(data: MutableList<SaySection>? = null) :
    BaseSectionQuickAdapter<SayAdapter.SaySection, BaseViewHolder>(R.layout.item_say, R.layout.item_say, data),
    FastScrollRecyclerView.SectionedAdapter, LoadMoreModule {
    private var pinnedIndex = 0

    /**
     * 关联RecyclerView
     * @param recyclerView RecyclerView
     * @return DragSelectTouchListener
     */
    fun setUpWithRecyclerView(container: StickyHeadContainer, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerView.adapter = this

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
//                onItemChildClickListener?.onItemChildClick(this, v, it)
            }
            avatar.setOnLongClickListener { v ->
//                onItemChildLongClickListener?.onItemChildLongClick(this, v, it) ?: false
                false
            }
            updateAvatar(avatar, item)
        }

        recyclerView.addItemDecoration(StickyItemDecoration(container, SectionEntity.HEADER_TYPE))
    }

    private val imageSizes = HashMap<String, Size>()
    private val largeContent = WeakHashMap<String, Spanned>()
    override fun convert(holder: BaseViewHolder, item: SaySection) {
//        holder.addOnClickListener(R.id.item_avatar)
//        holder.addOnLongClickListener(R.id.item_avatar)
        holder.itemView.item_user.text = item.t.user.name

        if (holder.adapterPosition == 0) holder.setBackgroundResource(R.id.item_layout, R.drawable.bg_round_dialog)
        else holder.setBackgroundColor(
            R.id.item_layout,
            ResourceUtil.resolveColorAttr(holder.itemView.context, android.R.attr.colorBackground)
        )

        updateAvatar(holder.itemView.item_avatar, item)
        val isSelf = item.t.user.username == UserModel.current()?.username
        val showAvatar = item.isHeader
        holder.itemView.item_avatar.visibility =
            if (showAvatar && pinnedIndex != holder.layoutPosition) View.VISIBLE else View.INVISIBLE
        (holder.itemView.item_avatar.layoutParams as ConstraintLayout.LayoutParams).let {
            it.horizontalBias = if (isSelf) 1f else 0f
        }

        (holder.itemView.item_user.layoutParams as ConstraintLayout.LayoutParams).let {
            it.startToStart = if (isSelf) ConstraintLayout.LayoutParams.UNSET else holder.itemView.item_message.id
            it.endToEnd = if (isSelf) holder.itemView.item_message.id else ConstraintLayout.LayoutParams.UNSET
        }

        val dpStart = ResourceUtil.toPixels(56f)
        val dpEnd = ResourceUtil.toPixels(64f)
        (holder.itemView.item_message.layoutParams as ConstraintLayout.LayoutParams).let {
            it.horizontalBias = if (isSelf) 1f else 0f
            it.marginEnd = if (isSelf) dpStart else dpEnd
            it.marginStart = if (isSelf) dpEnd else dpStart
        }
        holder.itemView.item_message.setBackgroundResource(if (isSelf) R.drawable.bg_say_right else R.drawable.bg_say_left)

        holder.itemView.item_user.visibility = if (item.isHeader && !isSelf) View.VISIBLE else View.GONE

        holder.itemView.item_message.let { item_message ->
            val makeSpan = {
                HtmlUtil.html2span(wrapHtmlBBCode(item.t.message),
                    HtmlUtil.ImageGetter(imageSizes) {
                        Math.min(holder.itemView.width.toFloat() - dpStart - dpEnd, Math.max(item_message.textSize, it))
                    }
                )
            }
            HtmlUtil.attachToTextView(largeContent.getOrPut(item.t.message, makeSpan), item_message)
        }

        holder.itemView.item_message.onFocusChangeListener = View.OnFocusChangeListener { view, focus ->
            if (!focus) {
                view.tag = null
                (view as TextView).text = view.text
            }
        }
        holder.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()
        holder.itemView.item_message.requestLayout()
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
    class SaySection(override var isHeader: Boolean, val t: Say.SayReply) : SectionEntity

    override fun addData(position: Int, data: SaySection) {
        data.isHeader = !(position > 0 && getItemOrNull(position - 1)?.t?.let {
            it.index == data.t.index - 1 && it.user.username == data.t.user.username
        } == true)
        super.addData(position, data)
        getItemOrNull(position + 1)?.let {
            if (it.isHeader && it.t.index == data.t.index + 1 && it.t.user.username == data.t.user.username) {
                it.isHeader = false
                notifyItemChanged(position + 1)
            }
        }
    }

    override fun setData(index: Int, data: SaySection) {
        data.isHeader = !(index > 0 && getItemOrNull(index - 1)?.t?.let {
            it.index == data.t.index - 1 && it.user.username == data.t.user.username
        } == true)
        super.setData(index, data)
        getItemOrNull(index + 1)?.let {
            if (it.isHeader && it.t.index == data.t.index + 1 && it.t.user.username == data.t.user.username) {
                it.isHeader = false
                notifyItemChanged(index + 1)
            }
        }
    }

    override fun convertHeader(helper: BaseViewHolder, item: SaySection) {
        convert(helper, item)
    }

    fun setNewData(say: Say) {
        super.setNewInstance(
            listOfNotNull(
                Say.SayReply(
                    user = say.user,
                    message = say.message ?: "",
                    index = 0
                )
            ).plus(say.replies ?: ArrayList()).let {
                it.mapIndexed { index, sayReply ->
                    SaySection(it.getOrNull(index - 1)?.user?.username != sayReply.user.username, sayReply)
                }
            }.toMutableList()
        )
    }

    /**
     * 时间线不支持BBCode，把如下的转换转回来
     * - 不直接用htmlToText是因为url会被重新处理：
     * ```
     * [img]...[/img] -> [img]<a href="...">...</a>[/img]
     * [url]...[/url] -> [url]<a href="...">...</a>[/url]
     * [url=...]...[/url] -> [url=<a href="...">...</a>]...[/url]
     * ```
     */
    fun wrapHtmlBBCode(html: String): String {
        val wrap = html.replace(
            Regex("""\[(img|url)]<a href="([^"]+)".*?</a>\[/\1]"""), "[$1]$2[/$1]"
        ).replace(Regex("""\[url=<a ([^>]*)>.*?</a>](.*?)\[/url]"""), "[url=$1]$2[/url]")
        return HtmlUtil.bbcode2html(HtmlUtil.span2bbcode(HtmlUtil.html2span(wrap)))
    }
}