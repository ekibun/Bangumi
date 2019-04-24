package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.AbstractExpandableItem
import com.chad.library.adapter.base.entity.MultiItemEntity

data class TopicPost(
        var pst_id: String = "",
        var pst_mid: String = "",
        var pst_uid: String = "",
        var pst_content: String = "",
        var username: String = "",
        var nickname: String = "",
        var sign: String = "",
        var avatar: String = "",
        var dateline: String = "",
        var is_self: Boolean = false,
        var isSub: Boolean = false,
        var editable: Boolean = false,
        var relate: String = "",
        val model: String = "",
        var floor: Int = 0,
        var sub_floor: Int = 0
):  AbstractExpandableItem<TopicPost>(), MultiItemEntity{
    override fun getItemType(): Int {
        return if(isSub) 1 else 0
    }
    override fun getLevel(): Int { return itemType }
}