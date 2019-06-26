package soko.ekibun.videoplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.videoplayer.IVideoSubjectProvider
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.callback.IListEpisodeCallback
import soko.ekibun.videoplayer.callback.IListSubjectCallback
import soko.ekibun.videoplayer.callback.ISubjectCallback

class VideoSubjectProvider : Service() {

    private var mVideoSubjectProvider = object: IVideoSubjectProvider.Stub() {
        override fun getSubjectSeason(subject: VideoSubject, callback: IListSubjectCallback) {
            val ua = subject.getUserAgent()
            val bgmSubject = subject.toSubject()
            BgmIpViewer.createInstance().subject(bgmSubject.id).enqueue(ApiHelper.buildCallback(null, {
                callback.onFinish(BgmIpViewer.getSeason(it, bgmSubject).map {
                    VideoSubject(Subject(it.subject_id, "${Bangumi.SERVER}/subject/${it.subject_id}", bgmSubject.type, it.name, it.name_cn), ua)
                })
            }, {}))
        }

        override fun refreshSubject(subject: VideoSubject, callback: ISubjectCallback) {
            val ua = subject.getUserAgent()
            Bangumi.getSubject(subject.toSubject(), ua?:"").enqueue(ApiHelper.buildCallback(null, {
                callback.onFinish(VideoSubject(it, ua))
            }, { it?.let{ callback.onReject(it.toString()) } }))
        }

        override fun refreshEpisode(subject: VideoSubject, callback: IListEpisodeCallback) {
            Bangumi.getSubjectEps(subject.id!!.toInt(),  subject.getUserAgent()?:"").enqueue(ApiHelper.buildCallback(null, { list ->
                callback.onFinish(list.map { VideoEpisode(it) })
            }, { it?.let{ callback.onReject(it.toString()) } }))
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mVideoSubjectProvider
    }
}
