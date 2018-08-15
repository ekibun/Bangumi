package soko.ekibun.bangumi.api.parser

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import kotlin.math.roundToInt

class BilibiliParser: Parser {
    override val siteId: Int = ParseInfo.BILIBILI

    override fun getVideoInfo(id: String, video: Episode): Call<Parser.VideoInfo> {
        val ids = id.split(" ")
        val vid = ids[0]
        val offset = ids.getOrNull(1)?.toFloatOrNull()?:0f
        return ApiHelper.buildHttpCall("https://bangumi.bilibili.com/anime/$vid", header){
            var body = it.body()?.string()?:""
            val start = "window.__INITIAL_STATE__="
            val end = "};"
            body = body.substring(body.indexOf(start)+start.length)
            body = body.substring(0, body.indexOf(end)+1)
            Log.v("body", body)
            val d = JsonUtil.toJsonObject(body)
            val episode = d.getAsJsonObject("mediaInfo").getAsJsonArray("episodes").get((video.sort + offset).toInt()-1).asJsonObject
            return@buildHttpCall Parser.VideoInfo(
                    episode.get("cid").toString(),
                    siteId,
                    "https://www.bilibili.com/bangumi/play/ep${episode.get("ep_id").asString}")
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): Call<String> {
        var url = api
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildHttpCall("https://comment.bilibili.com/${video.id}.xml", header) {
            val map: MutableMap<Int, MutableList<Parser.Danmaku>> = HashMap()
            val xml = String(IqiyiParser.inflate(it.body()!!.bytes(), true))
            Log.v("danmaku", xml)
            val doc = Jsoup.parse(xml)
            val infos = doc.select("d")
            for (info in infos) {
                val p = info.attr("p").split(",")
                val time = p.getOrNull(0)?.toFloat()?.roundToInt()?:0
                val color = "#" + String.format("%06x",  p.getOrNull(3)?.toLong()).toUpperCase()
                val context = info.text()
                val list: MutableList<Parser.Danmaku> = map[time] ?: ArrayList()
                val danmaku = Parser.Danmaku(context, time, color)
                Log.v("danmaku", danmaku.toString())
                list += danmaku
                map[time] = list
            }
            return@buildHttpCall map
        }
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}