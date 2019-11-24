package soko.ekibun.videoplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.videoplayer.IVideoSubjectProvider
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.callback.IListEpisodeCallback
import soko.ekibun.videoplayer.callback.IListSubjectCallback
import soko.ekibun.videoplayer.callback.ISubjectCallback

/**
 * 播放器ipc服务
 */
class VideoSubjectProvider : Service() {

    private var mVideoSubjectProvider = object : IVideoSubjectProvider.Stub() {
        override fun getSubjectSeason(subject: VideoSubject, callback: IListSubjectCallback) {
            val bgmSubject = subject.toSubject()
            BgmIpViewer.createInstance().subject(bgmSubject.id).enqueue(ApiHelper.buildCallback({
                callback.onFinish(BgmIpViewer.getSeason(it, bgmSubject).map { node -> VideoSubject(node) })
            }, {}))
        }

        override fun refreshSubject(subject: VideoSubject, callback: ISubjectCallback) {
            Subject.getDetail(subject.toSubject()).enqueue(ApiHelper.buildCallback({
                callback.onFinish(VideoSubject(it))
            }, { it?.let { callback.onReject(it.toString()) } }))
        }

        override fun refreshEpisode(subject: VideoSubject, callback: IListEpisodeCallback) {
            Episode.getSubjectEps(subject.toSubject()).enqueue(ApiHelper.buildCallback({ list ->
                callback.onFinish(list.map { VideoEpisode(it) })
            }, { it?.let { callback.onReject(it.toString()) } }))
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mVideoSubjectProvider
    }
}
