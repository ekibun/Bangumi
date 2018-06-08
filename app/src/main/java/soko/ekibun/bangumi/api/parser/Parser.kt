package soko.ekibun.bangumi.api.parser

import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView

interface Parser{
    val siteId: Int

    fun getVideoInfo(id: String, video: Episode, callback: (VideoInfo?)->Unit)

    fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo, callback: (String?) -> Unit)

    fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit)

    fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Danmaku>>?) -> Unit)

    data class VideoInfo(
            val id:String,
            val siteId: Int,
            val url:String
    )

    data class Danmaku(
            val context:String,
            val time: Int,
            val color: String
    )
}