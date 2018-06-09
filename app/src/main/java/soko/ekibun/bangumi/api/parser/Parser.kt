package soko.ekibun.bangumi.api.parser

import retrofit2.Call
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView

interface Parser{
    val siteId: Int

    fun getVideoInfo(id: String, video: Episode): Call<VideoInfo>

    fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): Call<String>

    fun getDanmakuKey(video: Parser.VideoInfo): Call<String>

    fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): Call<Map<Int, List<Danmaku>>>

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