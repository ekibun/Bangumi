package soko.ekibun.bangumi.ui.main.fragment.history

import android.view.View
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.entity.SectionEntity
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.oubowu.stickyitemdecoration.StickyHeadContainer
import com.oubowu.stickyitemdecoration.StickyItemDecoration
import kotlinx.android.synthetic.main.item_calendar.view.*
import kotlinx.android.synthetic.main.item_episode_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.model.history.History
import soko.ekibun.bangumi.util.GlideUtil

class HistoryAdapter(data: MutableList<HistorySection>? = null) :
    BaseSectionQuickAdapter<HistoryAdapter.HistorySection, BaseViewHolder>
        (R.layout.item_episode_header, R.layout.item_calendar, data), LoadMoreModule {

    class HistorySection(override val isHeader: Boolean) : SectionEntity {
        var header = ""
        var t: History? = null

        constructor(header: String) : this(true) {
            this.header = header
        }

        constructor(t: History) : this(false) {
            this.t = t
        }
    }

    override fun convertHeader(helper: BaseViewHolder, item: HistorySection) {
        helper.setText(R.id.item_header, item.header)
    }

    fun setUpWithRecyclerView(container: StickyHeadContainer, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerView.adapter = this
        container.setDataCallback {
            container.item_header.text = getItemOrNull(it)?.header
        }
        recyclerView.addItemDecoration(StickyItemDecoration(container, SectionEntity.HEADER_TYPE))
    }

    override fun convert(holder: BaseViewHolder, item: HistorySection) {
        holder.setText(R.id.item_title, item.t?.title)
        holder.setText(R.id.item_ep_name, item.t?.subTitle)
        GlideUtil.with(holder.itemView.item_cover)
            ?.load(Images.getImage(item.t?.thumb))
            ?.apply(
                RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder)
                    .transform(CenterCrop(), RoundedCorners(holder.itemView.item_cover.radius))
            )
            ?.into(holder.itemView.item_cover)
        holder.setText(R.id.item_time, item.t?.timeString)
        holder.itemView.item_time.visibility =
            if (getItemOrNull(holder.adapterPosition - 1)?.t?.timeString == item.t?.timeString)
                View.INVISIBLE else View.VISIBLE
        holder.itemView.item_del.visibility = View.VISIBLE
        holder.itemView.item_del.setOnClickListener {
            setOnItemChildClick(it, holder.layoutPosition)
        }
    }
}