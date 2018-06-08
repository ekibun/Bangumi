package soko.ekibun.bangumi.model

import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.parser.*
import soko.ekibun.bangumi.ui.view.BackgroundWebView

object ParseModel{
    private val parsers: Array<Parser> = arrayOf(
            IqiyiParser(),
            YoukuParser(),
            PptvParser(),
            TencentParser(),
            DilidiliParser())
    fun getVideoInfo(siteId:Int, id: String, video: Episode, callback: (Parser.VideoInfo?)->Unit){
        parsers.forEach {
            if(it.siteId == siteId) {
                it.getVideoInfo(id, video, callback)
                return
            } }
    }

    fun getVideo(siteId:Int, webView: BackgroundWebView, api: String, video: Parser.VideoInfo, callback: (String?) -> Unit){
        parsers.forEach {
            if(it.siteId == siteId) {
                it.getVideo(webView, api, video, callback)
                return
            } }
    }

    fun getDanmakuKey(siteId:Int, video: Parser.VideoInfo, callback: (String?) -> Unit){
        parsers.forEach {
            if(it.siteId == siteId) {
                it.getDanmakuKey(video, callback)
                return
            } }
    }

    fun getDanmaku(siteId:Int, video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit){
        parsers.forEach {
            if(it.siteId == siteId) {
                it.getDanmaku(video, key, pos, callback)
                return
            } }
    }
}