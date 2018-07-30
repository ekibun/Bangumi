package soko.ekibun.bangumi.ui.video

import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.api.parser.Parser
import soko.ekibun.bangumi.ui.view.DanmakuView

class DanmakuPresenter(val view: DanmakuView,
                       private val onFinish:(Throwable?)->Unit){
    private val danmakus = HashMap<Int, List<Parser.Danmaku>>()

    private var videoInfo: Parser.VideoInfo? = null
    private var danmakuKey: String = ""

    fun loadDanmaku(video: Parser.VideoInfo){
        danmakus.clear()
        view.clear()
        lastPos = -1
        videoInfo = video
        danmakuKey = ""
        ParseModel.getDanmakuKey(video.siteId, video).enqueue(ApiHelper.buildCallback(view.context, {
            danmakuKey = it
            add(0)
        },{}))
    }

    private var lastPos = -1
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos == -1 || lastPos != newPos){
            lastPos = newPos
            videoInfo?.let {
                ParseModel.getDanmaku(it.siteId, it, danmakuKey, (pos / 1000).toInt()).enqueue(ApiHelper.buildCallback(view.context, {
                    danmakus.putAll(it)
                }, {onFinish(it)}))
            }
        }
        view.add(danmakus[(pos/1000).toInt()]?: ArrayList())
    }
}