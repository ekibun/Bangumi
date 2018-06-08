package soko.ekibun.bangumi.parser

import android.util.Log
import com.google.gson.JsonNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.io.IOException

class PptvParser: Parser{
    override val siteId: Int = ParseInfo.PPTV

    override fun getVideoInfo(id: String, video: Episode, callback: (Parser.VideoInfo?) -> Unit) {
        HttpUtil.get("http://apis.web.pptv.com/show/videoList?format=jsonp&pid=$id", header, object: Callback{
            override fun onResponse(call: Call, response: Response) {
                try{
                    val src = JsonUtil.toJsonObject(response.body()?.string()?:"")
                    src.get("data").asJsonObject.get("list").asJsonArray?.map{it.asJsonObject}?.forEach {
                        Log.v("obj", it.toString())
                        if(it.get("title").asString.toFloatOrNull() == video.sort){
                            val info = Parser.VideoInfo(
                                    it.get("id").asString,
                                    siteId,
                                    it.get("url").asString
                            )
                            Log.v("video", info.toString())
                            callback(info)
                            return
                        } }
                }catch (e: Exception){ e.printStackTrace() }
                callback(null)
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        })
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo, callback: (String?) -> Unit) {
        webView.onCatchVideo={
            Log.v("video", it.url.toString())
            if(it.url.toString().contains("mdparse.duapp.com/404.mp4")){
                callback(it.url.toString())
            }else{
                callback(it.url.toString())
            }
            webView.onCatchVideo={}
        }
        var url = api//parseModel.sharedPreferences.getString("api_pptv", "")
        if(url.isEmpty())
            url = "https://jx.maoyun.tv/?id="
        if(url.endsWith("="))
            url += video.url.split("?")[0]

        val map = HashMap<String, String>()
        map["referer"]=url
        webView.loadUrl(url, map)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit) {
        callback("OK")
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val pageStart = pos / 300 * 3
        for(i in 0..5)
            getDanmaku(video, pageStart + i, callback)
    }

    private fun getDanmaku(video: Parser.VideoInfo, page: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val map: MutableMap<Int, MutableList<Parser.Danmaku>>? = HashMap()
        HttpUtil.get("http://apicdn.danmu.pptv.com/danmu/v4/pplive/ref/vod_${video.id}/danmu?pos=${page* 1000}", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val result = JsonUtil.toJsonObject(response.body()!!.string()).getAsJsonObject("data").getAsJsonArray("infos")
                    result.map{ it.asJsonObject}
                            .filter { it.get("id").asLong != 0L }
                            .forEach {
                                val time = it.get("play_point").asInt / 10
                                val fontclr = it.get("font_color")
                                val color = if(fontclr is JsonNull) "#FFFFFF" else fontclr.asString
                                val context = Jsoup.parse(it.get("content").asString).text()
                                val list: MutableList<Parser.Danmaku> = map?.get(time) ?: ArrayList()
                                val danmaku = Parser.Danmaku(context, time, color)
                                Log.v("danmaku", danmaku.toString())
                                list += danmaku
                                map?.put(time, list)
                            }
                }catch (e: Exception){ e.printStackTrace() }
                callback(map)
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        })
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