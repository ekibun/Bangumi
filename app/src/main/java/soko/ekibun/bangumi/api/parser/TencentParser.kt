package soko.ekibun.bangumi.api.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class TencentParser: Parser{
    override val siteId: Int = ParseInfo.TENCENT

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        val ids = id.split(" ")
        val vid = ids[0]
        val offset = ids.getOrNull(1)?.toFloatOrNull()?:0f
        return ApiHelper.buildHttpCall("https://s.video.qq.com/get_playsource?id=$vid&type=4&range=${(video.sort + offset).toInt()}-${(video.sort + offset).toInt()+1}&otype=json", header){
            var json = it.body()?.string()?:""
            json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            JsonUtil.toJsonObject(json).getAsJsonObject("PlaylistItem")
                    .getAsJsonArray("videoPlayList").map{it.asJsonObject}.forEach {
                        if(it.get("episode_number").asString.toFloatOrNull() == video.sort + offset && it.get("type").asString == "1"){
                            val info = Parser.VideoInfo(
                                    it.get("id").asString,
                                    siteId,
                                    it.get("playUrl").asString
                            )
                            Log.v("video", info.toString())
                            return@buildHttpCall info
                        } }
            throw Exception("not found")
        }
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        val apis = api.split(" ")
        var url = apis.getOrNull(0)?:""
        val js = apis.getOrNull(1)?:""
        if(url.isEmpty())
            url = video.url.split(".html?")[0] + "/${video.id}.html"
        else if(url.endsWith("="))
            url += video.url.split(".html?")[0] + "/${video.id}.html"
        return ApiHelper.buildWebViewCall(webView, url, header, js)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildHttpCall("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${video.id}", header){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            return@buildHttpCall doc.selectFirst("targetid").text()
        }
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        val list = ArrayList<retrofit2.Call<Map<Int, List<Parser.Danmaku>>>>()
        val pageStart = pos / 300 * 10
        for(i in 0..19)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildHttpCall("https://mfm.video.qq.com/danmu?timestamp=${page*30}&target_id=$key", header){
            val map: MutableMap<Int, MutableList<Parser.Danmaku>> = HashMap()
            val result = JsonUtil.toJsonObject(it.body()?.string()?:"").getAsJsonArray("comments")
            result.map{ it.asJsonObject}
                    .forEach {
                        val time = it.get("timepoint").asInt
                        val color = "#" + (it.get("bb_bcolor")?.asString?.replace("0x","")?:"FFFFFF")
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