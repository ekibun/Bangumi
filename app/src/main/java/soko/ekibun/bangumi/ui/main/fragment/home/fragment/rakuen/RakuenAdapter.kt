package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_topic.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Rakuen

class RakuenAdapter(data: MutableList<Rakuen>? = null) :
        BaseQuickAdapter<Rakuen, BaseViewHolder>(R.layout.item_topic, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: Rakuen) {
        helper.itemView.item_title.text = item.topic
        helper.itemView.item_time.text = "${item.time} ${item.plus}"
        helper.itemView.item_group.visibility = View.GONE
        item.group?.let{
            helper.itemView.item_group.visibility = View.VISIBLE
            helper.itemView.item_group.text = it
        }
        Glide.with(helper.itemView.item_avatar)
                .load(item.img)
                .apply(RequestOptions.circleCropTransform().error(R.drawable.ic_404))
                .into(helper.itemView.item_avatar)
    }
}