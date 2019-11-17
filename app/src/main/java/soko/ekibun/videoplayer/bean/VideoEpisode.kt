package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable
import soko.ekibun.bangumi.api.bangumi.bean.Episode

/**
 * 剧集（ipc）
 */
data class VideoEpisode(
        val site: String? = null,
        val id: String? = null,
        var url: String? = null,
        var cat: String? = null,
        var sort: Float = 0f,
        var name: String? = null,
        var duration: String? = null,
        var airdate: String? = null,
        var comment: Int = 0,
        var desc: String? = null,
        var status: String? = null,
        var progress: String? = null
) : Parcelable {

    /**
     * 数据转换
     */
    fun toEpisode(): Episode {
        return Episode(
                id =id?.toIntOrNull()?:0,
                category = cat,
                sort = sort,
                name = name,
                duration = duration,
                airdate = airdate,
                comment = comment,
                desc = desc,
                status = status,
                progress = mapOf(
                        "想看" to Episode.PROGRESS_QUEUE,
                        "在看" to Episode.PROGRESS_WATCH,
                        "抛弃" to Episode.PROGRESS_DROP
                )[progress]
        )
    }

    constructor(episode: Episode) : this(
            VideoSubject.BANGUMI_SITE,
            episode.id.toString(),
            episode.url,
            episode.category ?: when (episode.type) {
                Episode.TYPE_MAIN -> "本篇"
                Episode.TYPE_SP -> "特别篇"
                Episode.TYPE_OP -> "OP"
                Episode.TYPE_ED -> "ED"
                Episode.TYPE_PV -> "PV"
                Episode.TYPE_MAD -> "MAD"
                else -> "其他"
            },
            episode.sort,
            if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn,
            episode.duration,
            episode.airdate,
            episode.comment,
            episode.desc,
            episode.status,
            mapOf(
                    Episode.PROGRESS_QUEUE to "想看",
                    Episode.PROGRESS_WATCH to "在看",
                    Episode.PROGRESS_DROP to "抛弃"
            )[episode.progress]
    )

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readFloat(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(site)
        writeString(id)
        writeString(url)
        writeString(cat)
        writeFloat(sort)
        writeString(name)
        writeString(duration)
        writeString(airdate)
        writeInt(comment)
        writeString(desc)
        writeString(status)
        writeString(progress)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VideoEpisode> = object : Parcelable.Creator<VideoEpisode> {
            override fun createFromParcel(source: Parcel): VideoEpisode = VideoEpisode(source)
            override fun newArray(size: Int): Array<VideoEpisode?> = arrayOfNulls(size)
        }
    }
}