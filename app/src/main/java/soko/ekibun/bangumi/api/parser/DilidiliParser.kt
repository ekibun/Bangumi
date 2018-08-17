package soko.ekibun.bangumi.api.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView

class DilidiliParser: Parser {
    override val siteId: Int = ParseInfo.DILIDLILI

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        val ids = id.split(" ")
        val vid = ids[0]
        val offset = ids.getOrNull(1)?.toFloatOrNull()?:0f
        val num = id.split("/").getOrNull(2)?.toInt()?:0
        return ApiHelper.buildHttpCall("http://m.dilidili.wang/anime/$vid/", header){
            val d = Jsoup.parse(it.body()?.string()?:"")
            d.selectFirst(".episode").select("a").filter { it.text().toFloatOrNull() == video.sort + offset }.getOrNull(num)?.let {
                val url = it.attr("href")
                val info = Parser.VideoInfo(
                        Regex("""dilidili.wang/watch[0-9]?/([^/]*)/""").find(url)?.groupValues?.get(1)?:"",
                        siteId,
                        url
                )
                Log.v("video", info.toString())
                return@buildHttpCall info
            }
            throw Exception("not found")
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        val apis = api.split(" ")
        var url = apis.getOrNull(0)?:""
        val js = apis.getOrNull(1)?:""
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url, header, js)
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