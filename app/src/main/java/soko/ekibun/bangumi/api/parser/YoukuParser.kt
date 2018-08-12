package soko.ekibun.bangumi.api.parser

import android.util.Log

import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class YoukuParser: Parser{
    override val siteId: Int = ParseInfo.YOUKU

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        return ApiHelper.buildHttpCall("http://list.youku.com/show/episode?id=$id&stage=reload_${(video.sort.toInt()-1) / 10 * 10 + 1}&callback=jQuery", header){
            var json = it.body()?.string()?:""
            json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            val element = JsonUtil.toJsonObject(json)
            val li = Jsoup.parse(element.get("html").asString)
            li.select(".c555").forEach {
                Log.v("video", it.toString())
                if(it.parent().text().substringBefore(it.text()).toFloatOrNull() == video.sort){
                    val vid = Regex("""id_([^.=]+)""").find(it.attr("href"))?.groupValues?.get(1)?:"http:" + it.attr("href")
                    val info = Parser.VideoInfo(vid,
                            siteId,
                            "http:" + it.attr("href")
                    )
                    Log.v("video", info.toString())
                    return@buildHttpCall info
                } }
            throw Exception("not found")
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        var url = api
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildHttpCall(video.url, header){
            val doc = it.body()?.string()?:""
            return@buildHttpCall Regex("""videoId: '([0-9]+)'""").find(doc)!!.groupValues[1]
        }
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        val list = ArrayList<retrofit2.Call<Map<Int, List<Parser.Danmaku>>>>()
        val pageStart = pos / 300 * 5
        for(i in 0..9)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildHttpCall("http://service.danmu.youku.com/list?jsoncallback=&mat=$page&mcount=1&ct=1001&iid=$key", header){
            val map = HashMap<Int, MutableList<Parser.Danmaku>>()
            val result = JsonUtil.toJsonObject(it.body()?.string()?:"").getAsJsonArray("result")
            result.map{ it.asJsonObject}
                    .forEach {
                        val time = it.get("playat").asInt / 1000 //Integer.valueOf(info.selectFirst("showTime").text())
                        val property = it.get("propertis").asString

                        val color = "#" + if(property.contains("color")) String.format("%06x", JsonUtil.toJsonObject(property).get("color").asInt).toUpperCase() else "FFFFFF"//info.selectFirst("color").text().toUpperCase()
                        val context = it.get("content").asString
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
            map
        }
    }
}