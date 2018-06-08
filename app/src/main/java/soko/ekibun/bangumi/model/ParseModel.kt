package soko.ekibun.bangumi.model

import okhttp3.Response
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.parser.*
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException

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

    fun processUrl(url: String, callback: (ParseInfo.ParseItem)->Unit){
        when {
            url.contains("dilidili.wang") -> {
                val vid = Regex("""dilidili.wang/anime/([^/]*)/""").find(url)?.groupValues?.get(1)?:return
                callback(ParseInfo.ParseItem(ParseInfo.DILIDLILI, vid))
            }
            url.contains("iqiyi.com") -> HttpUtil.get(url, callback = object: okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call?, e: IOException?) {}
                override fun onResponse(call: okhttp3.Call?, response: Response) {
                    try {
                        val vid = Regex("""albumId: ([0-9]*),""").find(response.body()?.string()?:"")?.groupValues?.get(1) ?: return
                        callback(ParseInfo.ParseItem(ParseInfo.IQIYI, vid))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            url.contains("qq.com") -> {
                val vid = Regex("""qq.com/detail/\S/([^/.]*)""").find(url)?.groupValues?.get(1)
                        ?: Regex("""qq.com/cover/\S/([^/.]*)""").find(url)?.groupValues?.get(1)?: return
                callback(ParseInfo.ParseItem(ParseInfo.TENCENT, vid))
            }
            url.contains("youku.com") -> {
                val vid = Regex("""youku.com/show/id_z([^/.]*)""").find(url)?.groupValues?.get(1)?:return
                callback(ParseInfo.ParseItem(ParseInfo.YOUKU, vid))
            }
            url.contains("pptv.com") -> HttpUtil.get(url, callback = object: okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call?, e: IOException?) {}
                override fun onResponse(call: okhttp3.Call?, response: Response) {
                    try {
                        val vid = Regex(""""id":([0-9]*),""").find(response.body()?.string()?:"")?.groupValues?.get(1) ?: return
                        callback(ParseInfo.ParseItem(ParseInfo.PPTV, vid))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }
}