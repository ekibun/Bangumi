package soko.ekibun.bangumi.ui.video

import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.parser.Parser
import soko.ekibun.bangumi.ui.view.DanmakuView

class DanmakuPresenter(val view: DanmakuView,
                       private val onFinish:()->Unit){
    private val danmakus = HashMap<Int, List<Parser.Danmaku>>()

    private var videoInfo: Parser.VideoInfo? = null
    private var danmakuKey: String = ""

    val callback = { map: Map<Int, List<Parser.Danmaku>>? ->
        if(map!= null) {
            danmakus.putAll(map)
            finished = true
            onFinish()
        }
    }

    var finished: Boolean = false
    fun loadDanmaku(video: Parser.VideoInfo){
        finished = false
        danmakus.clear()
        view.clear()
        lastPos = -1
        videoInfo = video
        danmakuKey = ""
        ParseModel.getDanmakuKey(video.siteId, video){
            if(it != null) {
                danmakuKey = it
                add(0)
            }
        }
    }

    var lastPos = -1
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos == -1 || lastPos != newPos){
            lastPos = newPos
            videoInfo?.let {
                ParseModel.getDanmaku(it.siteId, it, danmakuKey, (pos / 1000).toInt(), callback)
            }
        }
        view.add(danmakus[(pos/1000).toInt()]?: ArrayList())
    }
}