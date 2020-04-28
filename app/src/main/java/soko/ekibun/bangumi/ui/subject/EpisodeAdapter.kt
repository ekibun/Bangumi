package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.entity.SectionEntity
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.oubowu.stickyitemdecoration.FullSpanUtil
import com.oubowu.stickyitemdecoration.StickyHeadContainer
import com.oubowu.stickyitemdecoration.StickyItemDecoration
import kotlinx.android.synthetic.main.item_episode.view.*
import kotlinx.android.synthetic.main.item_episode_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.DragSelectTouchListener
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 剧集列表Adapter
 * @property headerHeight Int
 * @property itemHeight Int
 * @property longClickListener Function1<Int, Boolean>
 * @property clickListener Function1<Int, Boolean>
 * @property updateSelection Function0<Unit>
 * @constructor
 */
class EpisodeAdapter(data: MutableList<SelectableSectionEntity<Episode>>? = null) :
    BaseSectionQuickAdapter<EpisodeAdapter.SelectableSectionEntity<Episode>, BaseViewHolder>
        (R.layout.item_episode_header, R.layout.item_episode, data), FastScrollRecyclerView.MeasurableAdapter,
    FastScrollRecyclerView.SectionedAdapter {
    override fun getSectionName(position: Int): String {
        return (getItemOrNull(position)?.t ?: getItemOrNull(position + 1)?.t)?.parseSort() ?: ""
    }

    override fun isFullSpan(position: Int): Boolean {
        return getItemViewType(position) == SectionEntity.HEADER_TYPE
    }

    private var headerHeight: Int = 0
    private var itemHeight: Int = 0
    override fun getItemHeight(position: Int): Int {
        return when (getItemViewType(position)) {
            SectionEntity.HEADER_TYPE -> headerHeight
            SectionEntity.NORMAL_TYPE -> itemHeight
            else -> 0
        }
    }

    /**
     * 可选中列表项
     * @param T
     * @property isSelected Boolean
     */
    class SelectableSectionEntity<T>(override val isHeader: Boolean) : SectionEntity {
        var isSelected = false
        var header = ""
        var t: T? = null

        constructor(header: String) : this(true) {
            this.header = header
        }

        constructor(t: T) : this(false) {
            this.t = t
        }
    }

    override fun convertHeader(helper: BaseViewHolder, item: SelectableSectionEntity<Episode>) {
        //helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
        if (headerHeight == 0) {
//            helper.itemView.measure(0, 0)
//            headerHeight = helper.itemView.measuredHeight + ((helper.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.topMargin + it.bottomMargin }?:0)
            headerHeight = ResourceUtil.toPixels(48f)
        }
    }

    override fun convert(holder: BaseViewHolder, item: SelectableSectionEntity<Episode>) {
        holder.setText(R.id.item_title, item.t?.parseSort())
        holder.setText(R.id.item_desc, if (item.t?.name_cn.isNullOrEmpty()) item.t?.name else item.t?.name_cn)
        val color = ResourceUtil.resolveColorAttr(
            holder.itemView.context,
            when {
                item.isSelected -> R.attr.colorPrimary
                else -> android.R.attr.textColorSecondary
            }
        )
        holder.itemView.item_select.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.item_title.setTextColor(color)
        holder.itemView.item_desc.setTextColor(color)
        holder.itemView.item_badge.visibility = if (item.t?.progress in arrayOf(
                Episode.PROGRESS_WATCH,
                Episode.PROGRESS_DROP,
                Episode.PROGRESS_QUEUE
            )
        ) View.VISIBLE else View.INVISIBLE
        holder.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(
            ResourceUtil.resolveColorAttr(
                holder.itemView.context,
                when (item.t?.progress) {
                    in listOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_QUEUE) -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                }
            )
        )
        holder.itemView.item_badge.text = mapOf(
            Episode.PROGRESS_WATCH to R.string.episode_status_watch,
            Episode.PROGRESS_DROP to R.string.episode_status_drop,
            Episode.PROGRESS_QUEUE to R.string.episode_status_wish
        )[item.t?.progress ?: ""]?.let { holder.itemView.context.getString(it) } ?: ""
        holder.itemView.ep_box.backgroundTintList = ColorStateList.valueOf(color)
        holder.itemView.ep_box.alpha = if ((item.t?.status ?: "") in listOf("Air")) 1f else 0.6f
//        holder.addOnClickListener(R.id.ep_box)
//        holder.addOnLongClickListener(R.id.ep_box)

        if (itemHeight == 0) {
            holder.itemView.measure(0, 0)
            itemHeight = holder.itemView.measuredHeight
        }
    }

    var longClickListener: (Int)-> Boolean = { false }
    var clickListener: (Int)->Boolean = { false }

    var updateSelection: ()->Unit = {}
    /**
     * 关联RecyclerView
     * @param recyclerView RecyclerView
     * @return DragSelectTouchListener
     */
    fun setUpWithRecyclerView(container: StickyHeadContainer, recyclerView: androidx.recyclerview.widget.RecyclerView): DragSelectTouchListener{
        recyclerView.adapter = this

        container.setDataCallback {
            container.item_header.text = getItemOrNull(it)?.header
        }

        recyclerView.addItemDecoration(StickyItemDecoration(container, SectionEntity.HEADER_TYPE))

        val touchListener = DragSelectTouchListener()
        recyclerView.addOnItemTouchListener(touchListener)
        longClickListener = { position ->
            getItem(position).let { model ->
                if (!model.isHeader) setSelect(position, true)
            }
            touchListener.setStartSelectPosition(position)
            true }
        clickListener = {position ->
            if(!data.none { it.isSelected }){
                getItem(position).let { model ->
                    if (!model.isHeader) setSelect(position, !model.isSelected)
                }
                false
            }else true
        }
        touchListener.selectListener = { start, end, isSelected ->
            for (i in start..end) setSelect(i, isSelected)
        }
        return touchListener
    }

    private fun setSelect(position: Int, isSelected: Boolean){
        val model = getItem(position)
        if (!model.isHeader && model.isSelected != isSelected) {
            model.isSelected = isSelected
            notifyItemChanged(position)
        }
        updateSelection()
    }

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SectionEntity.HEADER_TYPE)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SectionEntity.HEADER_TYPE)
    }
}