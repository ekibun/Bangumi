package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable

data class SubjectCache(
        val subject: VideoSubject,
        val videoList: List<VideoCache>
) : Parcelable {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    constructor(source: Parcel) : this(
            source.readParcelable<VideoSubject>(VideoSubject::class.java.classLoader),
            source.createTypedArrayList(VideoCache.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(subject, 0)
        writeTypedList(videoList)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SubjectCache> = object : Parcelable.Creator<SubjectCache> {
            override fun createFromParcel(source: Parcel): SubjectCache = SubjectCache(source)
            override fun newArray(size: Int): Array<SubjectCache?> = arrayOfNulls(size)
        }
    }
}