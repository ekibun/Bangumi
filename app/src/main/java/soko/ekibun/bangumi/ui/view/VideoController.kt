package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.controller_extra.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.controller.*
import java.util.*

class VideoController(view: ViewGroup,
                      private val onAction:(Controller.Action, param: Any)->Unit,
                      private val isFullScreen: ()->Boolean = {false}){
    private val ctrExtra: View by lazy {
        (view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.controller_extra, view, true)
    }

    init{
        var downX = 0f
        var downY = 0f
        var downPos = 0L
        var lastTime = 0L
        var dblclick = false
        view.setOnTouchListener { _, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    val curTime = System.currentTimeMillis()
                    if(curTime - lastTime < 200){
                        dblclick = true
                        onAction(Controller.Action.PLAY_PAUSE, 0)
                    }else{
                        dblclick = false
                    }
                    lastTime = curTime
                    downX = event.x
                    downY = event.y
                    downPos = position.toLong() * 10
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE->{
                    if(Math.abs(event.x -downX) > Math.abs(event.y -downY) && Math.abs(event.x -downX) >80){
                        doSkip = true//ctrlView.ctr_txt.visibility = View.VISIBLE
                        doShowHide(true)
                        resetTimeout(false)
                    }
                    updateProgress(downPos + (event.x - downX).toLong() * 66, true)
                    //controller.setProcessSkip(null, Math.abs(event.x -downX).toInt() * 66 / 10)
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP-> {
                    if (doSkip) {
                        onAction(Controller.Action.SEEK_TO, downPos + (event.x - downX).toLong() * 66)
                        doSkip = false
                    } else
                        if (!dblclick && System.currentTimeMillis() - lastTime < 200)
                            Handler().postDelayed({
                                if (!dblclick) {
                                    doShowHide(!isShow)
                                }
                            }, 200)
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    resetTimeout()
                }
            }
            true
        }
    }

    private val onClick = { action: Controller.Action ->
        resetTimeout()
        onAction(action, Unit)
    }
    private var doSkip = false
    private val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {
            doSkip = true
            resetTimeout(false)
        }
        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            updateProgress((progress / 10).toLong())
        }
        override fun onStopTrackingTouch(bar: SeekBar) {
            doSkip = false
            onAction(Controller.Action.SEEK_TO, bar.progress.toLong() * 10)
            resetTimeout()
        }
    }
    private val controller = hashMapOf(
            ctrSmall to SmallController(view, onClick, onSeekBarChangeListener),
            ctrLarge to LargeController(view, onClick, onSeekBarChangeListener)
    )

    val isShow get() = controller[if(isFullScreen()) ctrLarge else ctrSmall]?.ctrLayout?.visibility == View.VISIBLE

    fun doShowHide(show: Boolean) {
        val ctr = controller[if(isFullScreen()) ctrLarge else ctrSmall]
        if(show || ctrVisibility){
            for(i in controller.values)
                i.doShowHide(false)
            ctr?.doShowHide(show)
            onAction(if(show) Controller.Action.SHOW else Controller.Action.HIDE, Unit)
            if(show){ resetTimeout() }else{ timeoutTask?.cancel() }
        }
    }

    var ctrVisibility: Boolean = true
        set(v) {
            for(i in controller.values)
                i.setCtrVisibility(v)
            field = v
        }

    //timer
    var timer = Timer()
    private var timeoutTask: TimerTask? = null
    private fun resetTimeout(timeout: Boolean = true){
        //remove timeout task
        timeoutTask?.cancel()
        if(timeout){
            //add timeout task
            timeoutTask = object: TimerTask(){
                override fun run() {
                    doShowHide(false)
                }
            }
            timer.schedule(timeoutTask, 3000)
        }
    }

    var duration = 0
    var buffedPosition = 0
    private var position = 0
    fun updateProgress(posLong: Long, skip: Boolean = false){
        if(!doSkip || skip){
            position = Math.max(0, (posLong / 10).toInt())
            for(i in controller.values)
                i.updateProgress(position, duration, buffedPosition)
        }
    }

    fun updatePauseResume(isPlaying: Boolean) {
        for(i in controller.values)
            i.updatePauseResume(isPlaying)
    }

    fun updateDanmaku(show: Boolean){
        for(i in controller.values)
            i.updateDanmaku(show)
    }

    fun updateNext(hasNext: Boolean){
        for(i in controller.values)
            i.updateNext(hasNext)
    }

    fun setTitle(title: String){
        for(i in controller.values)
            i.setTitle(title)
    }

    fun updateLoading(show: Boolean){
        ctrExtra.ctr_load.visibility = if(show) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        private const val ctrSmall = 0
        private const val ctrLarge = 1
    }
}