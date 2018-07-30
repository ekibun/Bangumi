package soko.ekibun.bangumi.api.parser

import android.util.Log
import com.google.gson.JsonNull
import okhttp3.Request
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class PptvParser: Parser{
    override val siteId: Int = ParseInfo.PPTV

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        return ApiHelper.buildHttpCall("http://apis.web.pptv.com/show/videoList?format=jsonp&pid=$id", header){
            val src = JsonUtil.toJsonObject(it.body()?.string()?:"")
            src.get("data").asJsonObject.get("list").asJsonArray?.map{it.asJsonObject}?.forEach {
                Log.v("obj", it.toString())
                if(it.get("title").asString.toFloatOrNull() == video.sort){
                    val info = Parser.VideoInfo(
                            it.get("id").asString,
                            siteId,
                            it.get("url").asString
                    )
                    Log.v("video", info.toString())
                    return@buildHttpCall info
                } }
            throw Exception("not found")
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        var url = api
        if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        val list = ArrayList<retrofit2.Call<Map<Int, List<Parser.Danmaku>>>>()
        val pageStart = pos / 300 * 3
        for(i in 0..5)
            list.add(getDanmakuCall(video, pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(video: Parser.VideoInfo, page: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildHttpCall("http://apicdn.danmu.pptv.com/danmu/v4/pplive/ref/vod_${video.id}/danmu?pos=${page* 1000}", header){
            val map: MutableMap<Int, MutableList<Parser.Danmaku>> = HashMap()
            val result = JsonUtil.toJsonObject(it.body()!!.string()).getAsJsonObject("data").getAsJsonArray("infos")
            result.map{ it.asJsonObject}
                    .filter { it.get("id").asLong != 0L }
                    .forEach {
                        val time = it.get("play_point").asInt / 10
                        val fontClr = it.get("font_color")
                        val color = if(fontClr is JsonNull) "#FFFFFF" else fontClr.asString
                        val context = Jsoup.parse(it.get("content").asString).text()
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
            map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map["Cookie"] = "PUID=616193bbc9804a7dde16-12845cf9388b; __crt=1474886744633; ppi=302c3638; Hm_lvt_7adaa440f53512a144c13de93f4c22db=1475285458,1475556666,1475752293,1475913662"
            map
        }
    }
}