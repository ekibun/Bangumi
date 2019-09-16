package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable

data class VideoCache(
    val episode: VideoEpisode,
    val type: String,
    var contentLength: Long = 0L,
    var bytesDownloaded: Long = 0L,
    var percentDownloaded: Float = 0f
) : Parcelable {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    constructor(source: Parcel) : this(
            source.readParcelable<VideoEpisode>(VideoEpisode::class.java.classLoader),
            source.readString(),
            source.readLong(),
            source.readLong(),
            source.readFloat()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(episode, 0)
        writeString(type)
        writeLong(contentLength)
        writeLong(bytesDownloaded)
        writeFloat(percentDownloaded)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VideoCache> = object : Parcelable.Creator<VideoCache> {
            override fun createFromParcel(source: Parcel): VideoCache = VideoCache(source)
            override fun newArray(size: Int): Array<VideoCache?> = arrayOfNulls(size)
        }
    }
}