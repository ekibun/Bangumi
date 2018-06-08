package soko.ekibun.bangumi.model

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory


public class VideoModel(private val context: Context, private val onAction:(Action, Any)->Unit) {
    //private val parseModel: ParseModel by lazy{ ParseModel(context) }
    //private val videoCacheModel: VideoCacheModel by lazy{ App.getVideoCacheModel(context) }

    enum class Action {
        READY, BUFFERING, ENDED, VIDEO_SIZE_CHANGE
    }

    val player: SimpleExoPlayer by lazy{
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTackSelectionFactory)
        val player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
        player.addListener(object: Player.EventListener{
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
            override fun onSeekProcessed() {}
            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
            override fun onPlayerError(error: ExoPlaybackException?) {}
            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState){
                    Player.STATE_ENDED -> onAction(Action.ENDED, Unit)
                    Player.STATE_READY -> onAction(Action.READY, Unit)
                    Player.STATE_BUFFERING-> onAction(Action.BUFFERING, Unit)
                }
            }
        })
        player.addVideoListener(object: com.google.android.exoplayer2.video.VideoListener{
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                onAction(Action.VIDEO_SIZE_CHANGE, arrayOf(width, height, unappliedRotationDegrees, pixelWidthHeightRatio))
            }
            override fun onRenderedFirstFrame() {
                onAction(Action.READY, Unit)
            }
        })
        player
    }
/*
    fun playVideo(avBean: VideoInfo, surface: SurfaceView, callback: ()->Unit){
        val videoCache = videoCacheModel.getCache(avBean)
        if(videoCache != null){
            play(videoCache.url, surface, true)
        }else{
            parseModel.getVideo(avBean) { url: String, _ ->
                callback()
                play(url, surface, false)
            }
        }
    }*/

    fun play(url: String, surface: SurfaceView, cache: Boolean){
        player.setVideoSurfaceView(surface)
        val dataSourceFactory = DefaultHttpDataSourceFactory("exoplayer")/*if(cache) videoCacheModel.getCacheDataSourceFactory(url) else videoCacheModel.factory*/
        val videoSource = if(url.contains("m3u8"))
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
        else ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
        player.prepare(videoSource)
        player.playWhenReady = true
    }
}