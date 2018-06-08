package soko.ekibun.bangumi.ui.video

import android.content.pm.ActivityInfo
import android.view.View
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_player.*
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
                    next?.let{playNext(it)}
                    //context.viewpager.loadAv(nextAv)
                }
                Controller.Action.DANMAKU -> {

                    danmakuPresenter.view.visibility = if(danmakuPresenter.view.visibility == View.VISIBLE)
                        View.INVISIBLE else View.VISIBLE
                    controller.updateDanmaku(danmakuPresenter.view.visibility == View.VISIBLE)
                }
                Controller.Action.SEEKTO -> {
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
        VideoModel(context){ action: VideoModel.Action, param: Any ->
            when(action){
                VideoModel.Action.READY -> {
                    if(!controller.ctrVisibility){
                        controller.ctrVisibility = true
                        context.item_logcat.visibility = View.INVISIBLE
                        controller.doShowHide(false)
                    }
                    if(this.videoModel.player.playWhenReady)
                        doPlayPause(true)
                    if(!controller.isShow){
                        context.item_mask.visibility = View.INVISIBLE
                        context.toolbar.visibility = View.INVISIBLE
                    }
                    controller.updateLoading(false)
                }
                VideoModel.Action.BUFFERING -> controller.updateLoading(true)
                VideoModel.Action.ENDED -> doPlayPause(false)
                VideoModel.Action.VIDEO_SIZE_CHANGE -> {
                    val array = param as Array<*>
                    val width = array[0] as Int
                    val height = array[1] as Int
                    val pixelWidthHeightRatio = array[3] as Float
                    context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * width * pixelWidthHeightRatio/ height)) / context.video_surface.measuredWidth
                    context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * height * pixelWidthHeightRatio/ width)) / context.video_surface.measuredHeight
                }
            }
        }
    }

    init{
        context.app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            context.toolbar.visibility = if(verticalOffset != 0 || controller.isShow || context.video_surface_container.visibility != View.VISIBLE) View.VISIBLE else View.INVISIBLE
        }
    }

    private var loadVideo = false
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadDanmaku = false
        set(v) {
            field = v
            parseLogcat()
        }
    private fun parseLogcat(){
        context.runOnUiThread{
            context.item_logcat.text = "解析视频地址… " + (if(loadVideo) "【完成】" else "") + "\n" +
                    "全舰弹幕装填… "+ (if(danmakuPresenter.finished) "【完成】" else "") +
                    if(loadVideo) "\n开始视频缓冲…" else ""
        }
    }

    fun loadDanmaku(video: Parser.VideoInfo){
        danmakuPresenter.loadDanmaku(video)
    }

    var playNext: (Int)->Unit = {}
    var next: Int? = null
    fun play(episode: Episode, info: ParseInfo){
        loadVideo = false
        loadDanmaku = false
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
        context.nested_scroll.tag = true

        ParseModel.getVideoInfo(info.video?.type?:0, info.video?.id?:"", episode){ video->
            if(video != null) {
                context.runOnUiThread { ParseModel.getVideo(video.siteId, webView, info.api ?: "", video){
                    context.runOnUiThread{
                        it?.let{videoModel.play(it, context.video_surface, false)}
                        loadVideo = true
                    } } }
                if (info.danmaku?.type == info.video?.type && info.video?.id == info.danmaku?.id)
                    context.runOnUiThread { context.videoPresenter.loadDanmaku(video) }
                else
                    ParseModel.getVideoInfo(info.danmaku?.type ?: 0, info.danmaku?.id
                            ?: "", episode) { danmaku ->
                        if (danmaku != null) {
                            context.runOnUiThread { context.videoPresenter.loadDanmaku(danmaku) }
                        } } }
        }
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
    }
}