package soko.ekibun.bangumi.ui.search

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
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
        BaseQuickAdapter<MonoInfo, BaseViewHolder>(R.layout.item_mono, data) {

    override fun convert(helper: BaseViewHolder, item: MonoInfo) {
        helper.setText(R.id.item_title, if(item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        helper.setText(R.id.item_name_jp, item.name)
        helper.setText(R.id.item_summary, item.summary)
        GlideUtil.with(helper.itemView.item_cover)
            ?.load(Images.grid(item.image))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
                ?.into(helper.itemView.item_cover)
    }
}