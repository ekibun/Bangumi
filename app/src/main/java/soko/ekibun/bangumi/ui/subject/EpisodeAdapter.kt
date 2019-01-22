package soko.ekibun.bangumi.ui.subject

import android.content.res.ColorStateList
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.ui.view.DragSelectTouchListener

class EpisodeAdapter(data: MutableList<SelectableSectionEntity<Episode>>? = null) :
        BaseSectionQuickAdapter<EpisodeAdapter.SelectableSectionEntity<Episode>, BaseViewHolder>
        (R.layout.item_episode, R.layout.header_episode, data) {
    class SelectableSectionEntity<T>: SectionEntity<T>{
        var isSelected = false
        constructor(isHeader: Boolean, header: String): super(isHeader, header)
        constructor(t: T): super(t)
    }
    override fun convertHead(helper: BaseViewHolder, item: SelectableSectionEntity<Episode>) {
        helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
    }

    override fun convert(helper: BaseViewHolder, item: SelectableSectionEntity<Episode>) {
        helper.setText(R.id.item_title, item.t.parseSort())
        helper.setText(R.id.item_desc, if(item.t.name_cn.isNullOrEmpty()) item.t.name else item.t.name_cn)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.isSelected -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        helper.itemView.item_select.visibility = if(item.isSelected) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_ep_box.setPadding(helper.itemView.item_ep_box.paddingLeft,helper.itemView.item_ep_box.paddingTop,
                helper.itemView.item_ep_box.paddingLeft * if(item.isSelected) 4 else 1, helper.itemView.item_ep_box.paddingBottom)
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_badge.visibility = if(item.t.progress != null) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(
                ResourceUtil.resolveColorAttr(helper.itemView.context,
                        when {
                            item.t.progress?.status?.url_name?:"" in listOf(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE) -> R.attr.colorPrimary
                            else -> android.R.attr.textColorSecondary
                        }))
        helper.itemView.item_badge.text = item.t.progress?.status?.cn_name?:""
        helper.itemView.item_ep_box.backgroundTintList = ColorStateList.valueOf(color)
        helper.itemView.item_ep_box.alpha = if((item.t.status?:"") in listOf("Air"))1f else 0.6f
    }

    var longClickListener: (Int)-> Boolean = { false }
    var clickListener: (Int)->Boolean = { false }

    var updateSelection: ()->Unit = {}

    fun setUpWithRecyclerView(recyclerView: RecyclerView): DragSelectTouchListener{
        recyclerView.adapter = this
        val touchListener = DragSelectTouchListener()
        recyclerView.addOnItemTouchListener(touchListener)
        longClickListener = {position ->
            getItem(position)?.let{model ->
                if (!model.isHeader) setSelect(position, true) }
                touchListener.setStartSelectPosition(position)
            true }
        clickListener = {position ->
            if(!data.none { it.isSelected }){
                getItem(position)?.let{model ->
                    if (!model.isHeader) setSelect(position, !model.isSelected) }
                false
            }else true
        }
        touchListener.selectListener = { start, end, isSelected ->
            for (i in start..end) setSelect(i, isSelected)
        }
        return touchListener
    }

    private fun setSelect(position: Int, isSelected: Boolean){
        val model = getItem(position)?:return
        if (!model.isHeader && model.isSelected != isSelected) {
            model.isSelected = isSelected
            notifyItemChanged(position)
        }
        updateSelection()
    }
}