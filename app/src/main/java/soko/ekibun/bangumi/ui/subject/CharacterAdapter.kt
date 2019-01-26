package soko.ekibun.bangumi.ui.subject

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_character.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Character

class CharacterAdapter(data: MutableList<Character>? = null) :
        BaseQuickAdapter<Character, BaseViewHolder>(R.layout.item_character, data) {

    override fun convert(helper: BaseViewHolder, item: Character) {
        helper.setText(R.id.item_name, if(item.name_cn.isNullOrEmpty())item.name else item.name_cn)
        helper.setText(R.id.item_cv, item.actors?.map{if(it.name_cn.isNullOrEmpty())it.name else it.name_cn}?.reduce { acc, s -> "$acc/$s" })
        helper.setText(R.id.item_role, item.role_name)
        Glide.with(helper.itemView.item_avatar)
                .load(item.images?.grid)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)
    }
}