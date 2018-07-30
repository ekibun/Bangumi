package soko.ekibun.bangumi.api.parser

import android.util.Log
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class HalihaliParser: Parser {
    override val siteId: Int = ParseInfo.HALIHALI

    override fun getVideoInfo(id: String, video: Episode): retrofit2.Call<Parser.VideoInfo> {
        return ApiHelper.buildCall{Parser.VideoInfo(
                        video.sort.toString(),
                        siteId,
                        "http://m.halihali.cc/v/$id/0-${DecimalFormat("#.##").format(video.sort)}.html"
                )}
    }

    override fun getVideo(webView: BackgroundWebView, api: String, video: Parser.VideoInfo): retrofit2.Call<String> {
        Log.v("video", video.url)
        return ApiHelper.buildWebViewCall(webView, video.url)
    }

    override fun getDanmakuKey(video: Parser.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildGroupCall(arrayOf())
    }

    override fun getDanmaku(video: Parser.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<Parser.Danmaku>>> {
        return ApiHelper.buildGroupCall(arrayOf())
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}