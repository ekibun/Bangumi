package soko.ekibun.bangumi.api.parser

import android.util.Log
import okhttp3.Request
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater

class IqiyiParser: Parser{
    override val siteId: Int = ParseInfo.IQIYI

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        val ids = id.split("/")
        val vid = ids[0]
        val offset = ids.getOrNull(1)?.toFloatOrNull()?:0f
        return ApiHelper.buildHttpCall("http://mixer.video.iqiyi.com/jp/mixin/videos/avlist?albumId=$vid&page=${(video.sort + offset).toInt()/100 + 1}&size=100", header){
            var json = it.body()?.string()?:""
            if (json.startsWith("var"))
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            JsonUtil.toJsonObject(json).getAsJsonArray("mixinVideos").map{it.asJsonObject}.forEach {
                Log.v("obj", it.toString())
                if(it.get("order").asInt.toFloat() == video.sort + offset){
                    val info = Parser.VideoInfo(
                            it.get("tvId").asString,
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
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildGroupCall(arrayOf(
                getDanmakuCall(video, pos / 300 + 1),
                getDanmakuCall(video, pos / 300 + 2)
        ))
    }

    private fun getDanmakuCall(video: Parser.VideoInfo, page: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildHttpCall("http://cmts.iqiyi.com/bullet/${video.id.substring(video.id.length - 4, video.id.length - 2)}/${video.id.substring(video.id.length - 2, video.id.length)}/${video.id}_300_$page.z", header){
            val map: MutableMap<Int, MutableList<Parser.Danmaku>> = HashMap()
            val xml = String(inflate(it.body()!!.bytes()))
            val doc = Jsoup.parse(xml)
            val infos = doc.select("bulletInfo")
            for (info in infos) {
                val time = Integer.valueOf(info.selectFirst("showTime").text())
                val color = "#" + info.selectFirst("color").text().toUpperCase()
                val context = info.selectFirst("content").text()
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

        fun inflate(data: ByteArray, nowrap: Boolean = false): ByteArray {
            var output: ByteArray

            val inflater = Inflater(nowrap)
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