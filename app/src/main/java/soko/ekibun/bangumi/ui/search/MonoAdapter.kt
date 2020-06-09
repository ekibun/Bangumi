package soko.ekibun.bangumi.ui.search

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_mono.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.MonoInfo
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 人物Adapter
 * @constructor
 */
class MonoAdapter(data: MutableList<MonoInfo>? = null) :
    BaseQuickAdapter<MonoInfo, BaseViewHolder>(R.layout.item_mono, data), LoadMoreModule {

    override fun convert(holder: BaseViewHolder, item: MonoInfo) {
        holder.setText(R.id.item_title, if (item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        holder.setText(R.id.item_name_jp, item.name)
        holder.setText(R.id.item_summary, item.summary)
        GlideUtil.with(holder.itemView.item_cover)
            ?.load(Images.grid(item.image))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.into(holder.itemView.item_cover)
    }
}