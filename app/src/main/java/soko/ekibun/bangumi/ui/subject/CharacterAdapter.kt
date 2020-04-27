package soko.ekibun.bangumi.ui.subject

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_character.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Character
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 角色Adapter
 * @constructor
 */
class CharacterAdapter(data: MutableList<Character>? = null) :
        BaseQuickAdapter<Character, BaseViewHolder>(R.layout.item_character, data) {

    override fun convert(holder: BaseViewHolder, item: Character) {
        holder.setText(R.id.item_name, if (item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        holder.setText(
            R.id.item_cv,
            if (item.actors?.isNotEmpty() == true) item.actors.map { if (it.name_cn.isNullOrEmpty()) it.name else it.name_cn }
                .reduce { acc, s -> "$acc/$s" } else "")
        holder.setText(R.id.item_role, item.role_name)
        GlideUtil.with(holder.itemView.item_avatar)
            ?.load(Images.grid(item.image))
            ?.apply(
                RequestOptions.circleCropTransform().error(R.drawable.err_404).placeholder(R.drawable.placeholder_round)
            )
            ?.apply(RequestOptions.circleCropTransform())
            ?.into(holder.itemView.item_avatar)
    }
}