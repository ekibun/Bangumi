package soko.ekibun.bangumi.ui.main.fragment.cache

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.videoplayer.bean.SubjectCache

class CacheAdapter(data: MutableList<SubjectCache>? = null) :
        BaseQuickAdapter<SubjectCache, BaseViewHolder>
        (R.layout.item_subject, data) {
    override fun convert(helper: BaseViewHolder, item: SubjectCache) {
        helper.setText(R.id.item_title, item.subject.name)
        helper.setText(R.id.item_name_jp, helper.itemView.context.getString(R.string.parse_cache_eps, item.videoList.size))
        GlideUtil.with(helper.itemView.item_cover)
                ?.load(item.subject.image)
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.into(helper.itemView.item_cover)
    }
}