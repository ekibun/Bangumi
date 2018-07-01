package soko.ekibun.bangumi.ui.video

import android.content.pm.ActivityInfo
import android.support.design.widget.Snackbar
import android.view.View
import com.google.android.exoplayer2.ExoPlaybackException
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_player.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.model.VideoModel
import soko.ekibun.bangumi.api.parser.ParseInfo
import soko.ekibun.bangumi.api.parser.Parser
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.ui.view.VideoController
import soko.ekibun.bangumi.ui.view.controller.Controller
import java.util.*


class VideoPresenter(private val context: VideoActivity){

    val danmakuPresenter: DanmakuPresenter by lazy{
        DanmakuPresenter(context.danmaku_view){
            loadDanmaku = true
        }
    }

    val webView: BackgroundWebView by lazy{ BackgroundWebView(context) }

    val controller: VideoController by lazy{
        VideoController(context.controller_frame, { action: Controller.Action, param: Any ->
            when (action) {
                Controller.Action.PLAY_PAUSE -> doPlayPause(!videoModel.player.playWhenReady)
                Controller.Action.FULLSCREEN ->{
                    context.requestedOrientation = if(context.systemUIPresenter.isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                Controller.Action.NEXT -> {
                    next?.let{doPlay(it)}
                    //context.viewpager.loadAv(nextAv)
                }
                Controller.Action.DANMAKU -> {

                    danmakuPresenter.view.visibility = if(danmakuPresenter.view.visibility == View.VISIBLE)
                        View.INVISIBLE else View.VISIBLE
                    controller.updateDanmaku(danmakuPresenter.view.visibility == View.VISIBLE)
                }
                Controller.Action.SEEK_TO -> {
                    videoModel.player.seekTo(param as Long)
                    this.controller.updateProgress(videoModel.player.currentPosition)
                }
                Controller.Action.SHOW -> {
                    context.runOnUiThread{
                        updatePauseResume()
                        updateProgress()
                        context.item_mask.visibility = View.VISIBLE
                        context.toolbar.visibility = View.VISIBLE
                    }
                }
                Controller.Action.HIDE -> {
                    context.runOnUiThread{
                        context.item_mask.visibility = View.INVISIBLE
                        context.toolbar.visibility = View.INVISIBLE
                    }
                }
                Controller.Action.TITLE -> {
                    doPlayPause(false)
                    context.app_bar.setExpanded(false)
                    context.systemUIPresenter.appbarCollapsible(true)
                }
            }
        }, { context.systemUIPresenter.isLandscape })
    }
    val videoModel: VideoModel by lazy{
        VideoModel(context, object : VideoModel.Listener{
            override fun onReady(playWhenReady: Boolean) {
                if(!controller.ctrVisibility){
                    controller.ctrVisibility = true
                    context.item_logcat.visibility = View.INVISIBLE
                    controller.doShowHide(false)
                }
                if(playWhenReady)
                    doPlayPause(true)
                if(!controller.isShow){
                    context.item_mask.visibility = View.INVISIBLE
                    context.toolbar.visibility = View.INVISIBLE
                }
                controller.updateLoading(false)
            }
            override fun onBuffering() {
                controller.updateLoading(true)
            }
            override fun onEnded() {
                doPlayPause(false)
            }
            override fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * width * pixelWidthHeightRatio/ height)) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * height * pixelWidthHeightRatio/ width)) / context.video_surface.measuredHeight
            }
            override fun onError(error: ExoPlaybackException) {
                exception = error.sourceException
                Snackbar.make(context.root_layout, exception.toString(), Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    init{
        context.app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            context.toolbar.visibility = if(verticalOffset != 0 || controller.isShow || context.video_surface_container.visibility != View.VISIBLE) View.VISIBLE else View.INVISIBLE
        }
    }

    private var loadVideoInfo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadVideo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadDanmaku: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var exception: Exception? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private fun parseLogcat(){
        context.runOnUiThread{
            if(loadVideoInfo == false || loadVideo == false || exception != null)
                controller.updateLoading(false)
            context.item_logcat.text = "获取视频信息…" + if(loadVideoInfo == null) "" else (
                    if(loadVideoInfo != true) "【失败】" else ("【完成】" +
                            "\n解析视频地址…${if(loadVideo == null) "" else if(loadVideo == true) "【完成】" else "【失败】"}" +
                            "\n全舰弹幕装填…${if(loadDanmaku == null) "" else if(loadDanmaku == true) "【完成】" else "【失败】"}" +
                            if(loadVideo == true) "\n开始视频缓冲…" else "")) +
                    if(exception != null) "【失败】\n$exception" else ""
        }
    }

    fun loadDanmaku(video: Parser.VideoInfo){
        danmakuPresenter.loadDanmaku(video)
    }

    var doPlay: (Int)->Unit = {}
    var next: Int? = null
    var prev: Int? = null
    var videoCall: Call<Parser.VideoInfo>? = null
    fun play(episode: Episode, info: ParseInfo){
        context.systemUIPresenter.appbarCollapsible(false)
        loadVideoInfo = null
        loadVideo = null
        loadDanmaku = null
        exception = null
        controller.updateNext(next != null)
        videoModel.player.playWhenReady = false
        controller.updateLoading(true)
        context.video_surface_container.visibility = View.VISIBLE
        context.video_surface.visibility = View.VISIBLE
        context.controller_frame.visibility = View.VISIBLE
        controller.ctrVisibility = false
        context.item_logcat.visibility = View.VISIBLE
        controller.doShowHide(true)
        controller.setTitle(episode.parseSort() + " - " + if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn)
        playLoopTask?.cancel()
        //context.nested_scroll.tag = true

        videoCall?.cancel()
        if(info.video?.id.isNullOrEmpty()){
            videoModel.play(info.api?:"", context.video_surface)
            loadVideo = true
        }
        webView.loadUrl("about:blank")
        videoCall = ParseModel.getVideoInfo(info.video?.type?:0, info.video?.id?:"", episode)
        videoCall?.enqueue(ApiHelper.buildCallback(context,{video->
            context.runOnUiThread { ParseModel.getVideo(video.siteId, webView, info.api ?: "", video).enqueue(ApiHelper.buildCallback(context, {
                context.runOnUiThread{
                    it?.let{videoModel.play(it, context.video_surface)}
                    loadVideo = true
                } })) }
            if (info.danmaku?.type == info.video?.type && info.video?.id == info.danmaku?.id)
                context.runOnUiThread { context.videoPresenter.loadDanmaku(video) }
            else if(!info.danmaku?.id.isNullOrEmpty())
                ParseModel.getVideoInfo(info.danmaku?.type ?: 0, info.danmaku?.id
                        ?: "", episode).enqueue(ApiHelper.buildCallback(context, { danmaku ->
                    context.runOnUiThread { context.videoPresenter.loadDanmaku(danmaku) }
                }))
        },{ loadVideoInfo = it == null }))
    }

    private var playLoopTask: TimerTask? = null
    fun doPlayPause(play: Boolean){
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if(play){
            playLoopTask = object: TimerTask(){ override fun run() {
                updateProgress()
                danmakuPresenter.add(videoModel.player.currentPosition)
            } }
            controller.timer.schedule(playLoopTask, 0, 1000)
            context.video_surface.keepScreenOn = true
            if(!controller.isShow)context.toolbar.visibility = View.INVISIBLE
            danmakuPresenter.view.resume()
        }else{
            context.video_surface.keepScreenOn = false
            danmakuPresenter.view.pause()
        }
        context.systemUIPresenter.appbarCollapsible(!play)
    }

    private fun updateProgress(){
        controller.duration = videoModel.player.duration.toInt() /10
        controller.buffedPosition = videoModel.player.bufferedPosition.toInt() /10
        controller.updateProgress(videoModel.player.currentPosition)
    }

    private fun updatePauseResume() {
        controller.updatePauseResume(videoModel.player.playWhenReady)
        context.setPictureInPictureParams(!videoModel.player.playWhenReady)
    }
}