package soko.ekibun.bangumi.parser

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

class YoukuParser: Parser{
    override val siteId: Int = ParseInfo.YOUKU

    override fun getVideoInfo(id: String, video: Episode, callback: (Parser.VideoInfo?) -> Unit) {
        HttpUtil.get("http://list.youku.com/show/episode?id=$id&stage=reload_${video.sort.toInt()/10 + 1}&callback=jQuery", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    var json = response.body()?.string()?:""
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
            url += video.url

        val map = HashMap<String, String>()
        map["referer"]=url
        webView.loadUrl(url, map)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit) {
        HttpUtil.get(video.url, header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val doc = response.body()?.string()?:""
                    val iid = Regex("""videoId: &#39;([0-9]+)&#39""").find(doc)!!.groupValues[1]
                    callback(iid)
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
        val pageStart = pos / 300 * 5
        for(i in 0..9)
            getDanmakuAt(video, key,pageStart + i, callback)
    }

    fun getDanmakuAt(video: Parser.VideoInfo, key: String, page: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val map: MutableMap<Int, MutableList<Parser.Danmaku>>? = HashMap()
        HttpUtil.get("http://service.danmu.youku.com/list?jsoncallback=&mat=$page&mcount=1&ct=1001&iid=$key", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val result = JsonUtil.toJsonObject(response.body()?.string()?:"").getAsJsonArray("result")
                    result.map{ it.asJsonObject}
                            .forEach {
                                val time = it.get("playat").asInt / 1000 //Integer.valueOf(info.selectFirst("showTime").text())
                                val property = it.get("propertis").asString

                                val color = "#" + if(property.contains("color")) String.format("%06x", JsonUtil.toJsonObject(property).get("color").asInt).toUpperCase() else "FFFFFF"//info.selectFirst("color").text().toUpperCase()
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