package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress

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

    fun toEpisode(): Episode {
        return Episode(
                id =id?.toIntOrNull()?:0,
                url = url,
                cat = cat,
                sort = sort,
                name = name,
                duration = duration,
                airdate = airdate,
                comment = comment,
                desc = desc,
                status = status,
                progress = SubjectProgress.EpisodeProgress(
                        id?.toIntOrNull()?:0,
                        SubjectProgress.EpisodeProgress.EpisodeStatus.getByName(progress?:"")
                )
        )
    }

    constructor(episode: Episode) : this(
            VideoSubject.BANGUMI_SITE,
            episode.id.toString(),
            episode.url,
            episode.cat,
            episode.sort,
            if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn,
            episode.duration,
            episode.airdate,
            episode.comment,
            episode.desc,
            episode.status,
            episode.progress?.status?.cn_name
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