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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater

class IqiyiParser: Parser{
    override val siteId: Int = ParseInfo.IQIYI

    override fun getVideoInfo(id: String, video: Episode, callback: (Parser.VideoInfo?) -> Unit) {
        HttpUtil.get("http://mixer.video.iqiyi.com/jp/mixin/videos/avlist?albumId=$id&page=${video.sort.toInt()/100 + 1}&size=100", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    var json = response.body()?.string()?:""
                    if (json.startsWith("var"))
                        json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
                    JsonUtil.toJsonObject(json).getAsJsonArray("mixinVideos").map{it.asJsonObject}.forEach {
                        Log.v("obj", it.toString())
                        if(it.get("order").asInt.toFloat() == video.sort){
                            val info = Parser.VideoInfo(
                                    it.get("tvId").asString,
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
            if(it.url.toString().contains("/404.mp4")){
                callback(it.url.toString())
            }else{
                callback(it.url.toString())
            }
            webView.onCatchVideo={}
        }
        var url = api//parseModel.sharedPreferences.getString("api_pptv", "")
        if(url.isEmpty())
            url = "http://api.47ks.com/webcloud/?v="
        if(url.endsWith("="))
            url += video.url

        val map = HashMap<String, String>()
        map["referer"]=url
        webView.loadUrl(url, map)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit) {
        callback("OK")
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        getDanmaku(video, pos / 300 + 1, callback)
        getDanmaku(video, pos / 300 + 2, callback)
    }

    private fun getDanmaku(video: Parser.VideoInfo, page: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        val map: MutableMap<Int, MutableList<Parser.Danmaku>>? = HashMap()
        val tvId  = video.id
        HttpUtil.get("http://cmts.iqiyi.com/bullet/${tvId.substring(tvId.length - 4, tvId.length - 2)}/${tvId.substring(tvId.length - 2, tvId.length)}/${tvId}_300_$page.z", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val xml = String(inflate(response.body()!!.bytes()))
                    val doc = Jsoup.parse(xml)
                    val infos = doc.select("bulletInfo")
                    for (info in infos) {
                        val time = Integer.valueOf(info.selectFirst("showTime").text())
                        val color = "#" + info.selectFirst("color").text().toUpperCase()
                        val context = info.selectFirst("content").text()
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

        private fun inflate(data: ByteArray): ByteArray {
            var output: ByteArray

            val inflater = Inflater()
            inflater.reset()
            inflater.setInput(data)

            val o = ByteArrayOutputStream(data.size)
            try {
                val buf = ByteArray(1024)
                while (!inflater.finished()) {
                    val i = inflater.inflate(buf)
                    o.write(buf, 0, i)
                }
                output = o.toByteArray()
            } catch (e: java.lang.Exception) {
                output = data
                e.printStackTrace()
            } finally {
                try {
                    o.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            inflater.end()
            return output
        }
    }
}