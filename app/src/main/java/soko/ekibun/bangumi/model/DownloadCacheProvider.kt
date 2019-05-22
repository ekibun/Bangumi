package soko.ekibun.bangumi.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.videoplayer.IDownloadCacheProvider
import soko.ekibun.videoplayer.bean.SubjectCache
import soko.ekibun.videoplayer.bean.VideoCache
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.callback.IListSubjectCacheCallback
import soko.ekibun.videoplayer.callback.ISubjectCacheCallback
import soko.ekibun.videoplayer.callback.IVideoCacheCallback

class DownloadCacheProvider(val context: AppCompatActivity, val onServiceConnectionChange: (Boolean)->Unit): ServiceConnection {
    var aidl: IDownloadCacheProvider? = null

    fun bindService(){
        val aidlIntent = Intent("soko.ekibun.videoplayer.downloadcacheprovider")
        val resloveInfos = context.packageManager.queryIntentServices(aidlIntent, 0)
        if(resloveInfos.size < 1) return
        aidlIntent.component = ComponentName(resloveInfos[0].serviceInfo.packageName, resloveInfos[0].serviceInfo.name)
        context.bindService(aidlIntent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(){
        if(aidl != null) context.unbindService(this)
    }

    fun getCacheList(onFinish: (List<SubjectCache>)->Unit, onReject: (String)->Unit){
        aidl?.getCacheList("bangumi", object: IListSubjectCacheCallback.Stub() {
            override fun onFinish(result: MutableList<SubjectCache>) {
                onFinish(result)
            }
            override fun onReject(reason: String) {
                onReject(reason)
            }
        })?:{ onReject("aidl not initialized") }()
    }

    fun getSubjectCache(subject: Subject, onFinish: (SubjectCache)->Unit, onReject: (String)->Unit){
        aidl?.getSubjectCache(VideoSubject(subject, ""), object: ISubjectCacheCallback.Stub() {
            override fun onFinish(result: SubjectCache) {
                onFinish(result)
            }
            override fun onReject(reason: String) {
                onReject(reason)
            }
        })?:{ onReject("aidl not initialized") }()
    }

    fun getEpisodeCache(subject: Subject, episode: Episode, onFinish: (VideoCache)->Unit, onReject: (String)->Unit){
        aidl?.getEpisodeCache(VideoSubject(subject, ""), VideoEpisode(episode), object: IVideoCacheCallback.Stub() {
            override fun onFinish(result: VideoCache) {
                onFinish(result)
            }
            override fun onReject(reason: String) {
                onReject(reason)
            }
        })?:{ onReject("aidl not initialized") }()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        aidl = null
        onServiceConnectionChange(false)
        if(!context.isDestroyed) bindService()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        aidl = IDownloadCacheProvider.Stub.asInterface(service)
        onServiceConnectionChange(true)
    }
}