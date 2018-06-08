package soko.ekibun.bangumi.api.parser

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.io.IOException

class TencentParser: Parser{
    override val siteId: Int = ParseInfo.TENCENT

    override fun getVideoInfo(id: String, video: Episode, callback: (Parser.VideoInfo?) -> Unit) {
        HttpUtil.get("https://s.video.qq.com/get_playsource?id=$id&type=4&range=${video.sort.toInt()}-${video.sort.toInt()+1}&otype=json", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    var json = response.body()?.string()?:""
                    json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
                    JsonUtil.toJsonObject(json).getAsJsonObject("PlaylistItem")
                            .getAsJsonArray("videoPlayList").map{it.asJsonObject}.forEach {
                        Log.v("obj", it.toString())
                        if(it.get("episode_number").asString.toFloatOrNull() == video.sort && it.get("type").asString == "1"){
                            val info = Parser.VideoInfo(
                                    it.get("id").asString,
                                    siteId,
                                    it.get("playUrl").asString
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
            if(it.url.toString().contains("/404.mp4")){
                callback(it.url.toString())
            }else{
                callback(it.url.toString())
            }
            webView.onCatchVideo={}
        }
        var url = api
        if(url.isEmpty())
            url = "http://jx.myxit.cn/vip/?url="
        if(url.endsWith("="))
            url += video.url.split(".html?")[0] + "/${video.id}.html"

        val map = HashMap<String, String>()
        map["referer"]=url
        webView.loadUrl(url, map)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit) {
        HttpUtil.get("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${video.id}", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val doc = Jsoup.parse(response.body()?.string()?:"")
                    val key = doc.selectFirst("targetid").text()
                    callback(key)
                    return
                }catch (e: Exception){ e.printStackTrace() }
                callback(null)
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        })
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val pageStart = pos / 300 * 10
        for(i in 0..19)
            getDanmakuAt(video, key, pageStart + i, callback)
    }

    fun getDanmakuAt(video: Parser.VideoInfo, key: String, page: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val map: MutableMap<Int, MutableList<Parser.Danmaku>>? = HashMap()
        HttpUtil.get("https://mfm.video.qq.com/danmu?timestamp=${page*30}&target_id=$key", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val result = JsonUtil.toJsonObject(response.body()?.string()?:"").getAsJsonArray("comments")
                    result.map{ it.asJsonObject}
                            .forEach {
                                val time = it.get("timepoint").asInt
                                val color = "#" + (it.get("bb_bcolor")?.asString?.replace("0x","")?:"FFFFFF")
                                val context = it.get("content").asString
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
            map
        }
    }
}