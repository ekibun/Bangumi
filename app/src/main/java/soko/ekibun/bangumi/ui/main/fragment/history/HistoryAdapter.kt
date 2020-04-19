package soko.ekibun.bangumi.ui.main.fragment.history

import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oubowu.stickyitemdecoration.StickyHeadContainer
import com.oubowu.stickyitemdecoration.StickyItemDecoration
import kotlinx.android.synthetic.main.item_calendar.view.*
import kotlinx.android.synthetic.main.item_episode_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.util.GlideUtil

class HistoryAdapter(data: MutableList<History>? = null) :
    BaseSectionQuickAdapter<HistoryAdapter.History, BaseViewHolder>
        (R.layout.item_calendar, R.layout.item_episode_header, data) {

    class History : SectionEntity<HistoryModel.History> {
        constructor(header: String) : super(true, header)
        constructor(t: HistoryModel.History) : super(t)
    }

    override fun convertHead(helper: BaseViewHolder, item: History) {
        helper.setText(R.id.item_header, item.header)
    }

    fun setUpWithRecyclerView(container: StickyHeadContainer, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        bindToRecyclerView(recyclerView)
        container.setDataCallback {
            container.item_header.text = data.getOrNull(it)?.header
        }
        recyclerView.addItemDecoration(StickyItemDecoration(container, SECTION_HEADER_VIEW))
    }

    override fun convert(helper: BaseViewHolder, item: History) {
        helper.setText(R.id.item_title, item.t.title)
        helper.setText(R.id.item_ep_name, item.t.subTitle)
        GlideUtil.with(helper.itemView.item_cover)
            ?.load(Images.getImage(item.t.thumb))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(helper.itemView.item_cover)
        helper.setText(R.id.item_time, item.t.timeString)
        helper.itemView.item_time.visibility =
            if (data.getOrNull(helper.adapterPosition - 1)?.t?.timeString == item.t.timeString)
                View.INVISIBLE else View.VISIBLE
        helper.addOnClickListener(R.id.item_del)
    }
}