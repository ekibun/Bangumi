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
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory


class VideoModel(private val context: Context, private val onAction: Listener) {
    //private val parseModel: ParseModel by lazy{ ParseModel(context) }
    //private val videoCacheModel: VideoCacheModel by lazy{ App.getVideoCacheModel(context) }

    interface Listener{
        fun onReady(playWhenReady: Boolean)
        fun onBuffering()
        fun onEnded()
        fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
        fun onError(error: ExoPlaybackException)
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
            override fun onPlayerError(error: ExoPlaybackException) { onAction.onError(error) }
            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState){
                    Player.STATE_ENDED -> onAction.onEnded()
                    Player.STATE_READY -> onAction.onReady(playWhenReady)
                    Player.STATE_BUFFERING-> onAction.onBuffering()
                }
            }
        })
        player.addVideoListener(object: com.google.android.exoplayer2.video.VideoListener{
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                onAction.onVideoSizeChange(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
            }
            override fun onRenderedFirstFrame() {
                //onAction.onReady(player.playWhenReady)
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

    fun play(url: String, surface: SurfaceView){
        player.setVideoSurfaceView(surface)
        val dataSourceFactory = DefaultHttpDataSourceFactory("exoplayer", null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true)/*if(cache) videoCacheModel.getCacheDataSourceFactory(url) else videoCacheModel.factory*/
        val videoSource = if(url.contains("m3u8"))
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
        else ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
        player.prepare(videoSource)
        player.playWhenReady = true
    }
}