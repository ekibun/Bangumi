package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 条目(ipc)
 */
data class VideoSubject(
        val site: String? = null,
        val id: String? = null,
        var url: String? = null,
        var image: String? = null,
        var name: String? = null,
        var type: String? = null,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var rank: Int = 0,
        var rating: Float = 0f,
        var rating_count: Int = 0,
        var desc: String? = null,
        var eps: List<VideoEpisode>? = null,
        var collect: String? = null,
        var token: String? = null
) : Parcelable {

    /**
     * 附加信息
     */
    data class Token(
            var formhash: String?,
            var collect: Collection?,
            @Subject.SubjectType var type: String?
    )

    /**
     * 数据转换
     */
    fun toSubject(): Subject{
        val data = JsonUtil.toEntity(token?:"", Token::class.java)
        HttpUtil.formhash = data?.formhash ?: HttpUtil.formhash
        return Subject(
                id = id?.toIntOrNull()?:0,
                images = Images(image ?: ""),
                name = name,
                type = data?.type ?: Subject.TYPE_ANY,
                category = type,
                air_date = air_date,
                air_weekday = air_weekday,
                rating = Subject.UserRating(
                        rank = rank,
                        total = rating_count,
                        score = rating
                ),
                summary = desc,
                eps = eps?.map { it.toEpisode() },
                collect = data?.collect
        )
    }

    constructor(subject: Subject) : this(BANGUMI_SITE,
            subject.id.toString(),
            subject.url,
            subject.images?.common,
            subject.displayName,
            subject.category,
            subject.air_date,
            subject.air_weekday,
            subject.rating?.rank ?: 0,
            subject.rating?.score ?: 0f,
            subject.rating?.total?:0,
            subject.summary,
            subject.eps?.map { VideoEpisode(it) },
            when (subject.collect?.status) {
                Collection.TYPE_WISH -> "想看"
                Collection.TYPE_COLLECT -> "看过"
                Collection.TYPE_DO -> "在看"
                Collection.TYPE_ON_HOLD -> "搁置"
                Collection.TYPE_DROPPED -> "抛弃"
                else -> null
            },
            JsonUtil.toJson(Token(HttpUtil.formhash, subject.collect, subject.type))
    )

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readFloat(),
            source.readInt(),
            source.readString(),
            source.createTypedArrayList(VideoEpisode.CREATOR),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(site)
        writeString(id)
        writeString(url)
        writeString(image)
        writeString(name)
        writeString(type)
        writeString(air_date)
        writeInt(air_weekday)
        writeInt(rank)
        writeFloat(rating)
        writeInt(rating_count)
        writeString(desc)
        writeTypedList(eps)
        writeString(collect)
        writeString(token)
    }

    companion object {
        const val BANGUMI_SITE = "bangumi"

        @JvmField
        val CREATOR: Parcelable.Creator<VideoSubject> = object : Parcelable.Creator<VideoSubject> {
            override fun createFromParcel(source: Parcel): VideoSubject = VideoSubject(source)
            override fun newArray(size: Int): Array<VideoSubject?> = arrayOfNulls(size)
        }
    }
}