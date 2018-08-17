package soko.ekibun.bangumi.api.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class Anime1Parser: Parser {
    override val siteId: Int = ParseInfo.ANIME1

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        val ids = id.split(" ")
        val vid = ids[0]
        return ApiHelper.buildHttpCall(vid, header){
            val d = Jsoup.parse(it.body()?.string()?:"")
            val src = d.selectFirst("iframe")?.attr("src")?:throw Exception("not found")
            val info = Parser.VideoInfo(
                    vid, siteId, src)
            Log.v("video", info.toString())
            return@buildHttpCall info
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        Log.v("video", video.url)
        val apis = api.split(" ")
        var url = apis.getOrNull(0)?:""
        val js = apis.getOrNull(1)?:"jwplayer().getPlaylist()[0].sources[1].file"
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return if(video.id.toIntOrNull()?:0 == 0) ApiHelper.buildWebViewCall(webView, url, header, js) else ApiHelper.buildCall { url }
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildGroupCall(arrayOf())
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildGroupCall(arrayOf())
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}