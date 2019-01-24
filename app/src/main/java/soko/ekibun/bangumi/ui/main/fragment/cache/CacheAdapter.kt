package soko.ekibun.bangumi.ui.main.fragment.cache

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.PlayerBridge

class CacheAdapter(data: MutableList<PlayerBridge.VideoCache>? = null) :
        BaseQuickAdapter<PlayerBridge.VideoCache, BaseViewHolder>
        (R.layout.item_subject, data) {
    override fun convert(helper: BaseViewHolder, item: PlayerBridge.VideoCache) {
        helper.setText(R.id.item_title, item.bangumi.getPrettyName())
        helper.setText(R.id.item_name_jp, item.bangumi.name)
        helper.setText(R.id.item_summary, "已缓存 ${item.videoList.size} 话")
        Glide.with(helper.itemView.item_cover)
                .load(item.bangumi.images?.getImage(helper.itemView.context))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
    }
}