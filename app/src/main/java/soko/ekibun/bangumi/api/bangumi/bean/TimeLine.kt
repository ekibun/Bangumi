package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.SectionEntity

class TimeLine: SectionEntity<TimeLine.TimeLineItem> {
    constructor(isHeader: Boolean, header: String): super(isHeader, header)
    constructor(t: TimeLineItem): super(t)

    data class TimeLineItem(
            val user: UserInfo,
            val action: String,
            val time: String,
            val content: String?,
            val contentUrl: String?,
            val collectStar: Int,
            val thumbs: List<ThumbItem>,
            val delUrl: String?,
            val sayUrl: String?
    ){
        data class ThumbItem(
            val image: String,
            val title: String,
            val url: String
        )
    }
}