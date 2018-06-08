package soko.ekibun.bangumi.parser

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.HttpUtil
import java.io.IOException

class DilidiliParser: Parser {
    override val siteId: Int = ParseInfo.DILIDLILI

    override fun getVideoInfo(id: String, video: Episode, callback: (Parser.VideoInfo?) -> Unit) {
        HttpUtil.get("http://m.dilidili.wang/anime/$id/", header, object: Callback {
            override fun onResponse(call: Call, response: Response) {
                try{
                    val d = Jsoup.parse(response.body()?.string()?:"")
                    d.selectFirst(".episodeWrap").select("a").forEach {
                        if(it.text().toFloatOrNull() == video.sort){
                            val url = it.attr("href")
                            val info = Parser.VideoInfo(
                                    Regex("""dilidili.wang/watch[0-9]?/([^/]*)/""").find(url)?.groupValues?.get(1)?:"",
                                    siteId,
                                    url
                            )
                            Log.v("video", info.toString())
                            callback(info)
                            return
                        }}
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
        val url = video.url
        val map = HashMap<String, String>()
        map["referer"]=url
        webView.loadUrl(url, map)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo, callback: (String?) -> Unit) {
        //not supported
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int, callback: (Map<Int, List<Parser.Danmaku>>?) -> Unit) {
        //not supported
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}