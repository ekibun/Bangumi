package soko.ekibun.bangumi.model

import okhttp3.Response
import retrofit2.Call
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
            DilidiliParser(),
            HalihaliParser(),
            BilibiliParser())
    fun getVideoInfo(siteId:Int, id: String, video: Episode): Call<Parser.VideoInfo> {
        parsers.forEach {
            if(it.siteId == siteId) {
                return it.getVideoInfo(id, video)
            } }
        throw Exception("no such parser")
    }

    fun getVideo(siteId:Int, webView: BackgroundWebView, api: String, video: Parser.VideoInfo) : Call<String>{
        parsers.forEach {
            if(it.siteId == siteId) {
                return it.getVideo(webView, api, video)
            } }
        throw Exception("no such parser")
    }

    fun getDanmakuKey(siteId:Int, video: Parser.VideoInfo) : Call<String>{
        parsers.forEach {
            if(it.siteId == siteId) {
                return it.getDanmakuKey(video)
            } }
        throw Exception("no such parser")
    }

    fun getDanmaku(siteId:Int, video: Parser.VideoInfo, key: String, pos: Int): Call<Map<Int, List<Parser.Danmaku>>>{
        parsers.forEach {
            if(it.siteId == siteId) {
                return it.getDanmaku(video, key, pos)
            } }
        throw Exception("no such parser")
    }

    fun processUrl(url: String, callback: (ParseInfo.ParseItem)->Unit){
        when {
            url.contains("bilibili.com") -> {
                val vid = Regex("""([0-9]*)$""").find(url)?.groupValues?.get(1)?:return
                callback(ParseInfo.ParseItem(ParseInfo.BILIBILI, vid))
            }
            url.contains("halihali.cc") -> {
                val vid = Regex("""halihali.cc/v/([^/]*)/""").find(url)?.groupValues?.get(1)?:return
                callback(ParseInfo.ParseItem(ParseInfo.DILIDLILI, vid))
            }
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