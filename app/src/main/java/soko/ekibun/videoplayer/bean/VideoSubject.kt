package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.JsonUtil

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

    data class Token(
        var formhash: String?,
        var ua: String?,
        var interest: Collection?
    )

    fun getUserAgent(): String? {
        return JsonUtil.toEntity(token?:"", Token::class.java)?.ua
    }

    fun toSubject(): Subject{
        val data = JsonUtil.toEntity(token?:"", Token::class.java)
        return Subject(
                id = id?.toIntOrNull()?:0,
                url = url,
                images = Images(image, image, image, image, image),
                name = name,
                typeString = type,
                air_date = air_date,
                air_weekday = air_weekday,
                rank = rank,
                rating = Subject.RatingBean(rating_count, null, rating.toDouble()),
                summary = desc,
                eps = eps?.map { it.toEpisode() },
                interest = data?.interest,
                formhash = data?.formhash
        )
    }

    constructor(subject: Subject, ua: String?) : this(BANGUMI_SITE,
            subject.id.toString(),
            subject.url,
            subject.images?.common,
            subject.getPrettyName(),
            subject.typeString,
            subject.air_date,
            subject.air_weekday,
            subject.rank,
            subject.rating?.score?.toFloat()?:0f,
            subject.rating?.total?:0,
            subject.summary,
            (subject.eps as? List<*>)?.mapNotNull { VideoEpisode(it as? Episode?:return@mapNotNull null) },
            subject.interest?.status?.name,
            JsonUtil.toJson(Token(subject.formhash, ua, subject.interest))
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