package soko.ekibun.bangumi.ui.view.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import soko.ekibun.bangumi.R
import java.lang.StringBuilder
import java.util.*

abstract class Controller(layoutRes: Int, view: ViewGroup) {
    protected val ctrView: View by lazy {
        (view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(layoutRes, view, false)
    }
    abstract val ctrLayout: View
    abstract val ctrPlayPause: ImageButton
    abstract val ctrTimeText: TextView
    abstract val ctrFullscreen: ImageButton
    abstract val ctrSeekBar: SeekBar
    abstract val ctrTitleText: TextView?
    abstract val ctrNext: ImageButton?
    abstract val ctrDanmaku: ImageButton?

    enum class Action {
        PLAY_PAUSE, FULLSCREEN, NEXT, DANMAKU, SEEK_TO, SHOW, HIDE, TITLE
    }

    fun initView(view: ViewGroup, onClick:(Action)->Unit, onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener){
        ctrPlayPause.setOnClickListener {
            onClick(Action.PLAY_PAUSE)
        }
        ctrFullscreen.setOnClickListener {
            onClick(Action.FULLSCREEN)
        }
        ctrNext?.setOnClickListener {
            onClick(Action.NEXT)
        }
        ctrDanmaku?.setOnClickListener {
            onClick(Action.DANMAKU)
        }
        ctrTitleText?.setOnClickListener {
            onClick(Action.TITLE)
        }
        ctrSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
        ctrLayout.visibility = View.INVISIBLE
        //add to frame
        view.addView(ctrView)
    }

    fun doShowHide(show: Boolean) {
        ctrView.post{ ctrLayout.visibility = if(show) View.VISIBLE else View.GONE }
    }

    fun setCtrVisibility(showAll: Boolean) {
        val visibility = if(showAll) View.VISIBLE else View.INVISIBLE
        ctrView.post{
            ctrPlayPause.visibility = visibility
            ctrTimeText.visibility = visibility
            ctrSeekBar.visibility = visibility
            ctrTitleText?.visibility = visibility
            ctrNext?.visibility = visibility
            ctrDanmaku?.visibility = visibility }
    }

    fun updateProgress(pos: Int, dur: Int, buf: Int){
        ctrView.post{
            ctrSeekBar.max = dur
            ctrSeekBar.progress = pos
            ctrSeekBar.secondaryProgress = buf
            ctrTimeText.text = ctrTimeText.context.getString(R.string.phrase_time, stringForTime(pos), stringForTime(dur)) }
    }

    fun updatePauseResume(isPlaying: Boolean) {
        ctrView.post { ctrPlayPause.setImageResource(if(isPlaying) R.drawable.ic_pause else R.drawable.ic_play) }
    }

    fun updateDanmaku(show: Boolean) {
        ctrView.post { ctrDanmaku?.alpha = if(show) 1f else 0.5f }
    }

    fun updateNext(hasNext: Boolean) {
        ctrView.post { ctrNext?.alpha = if(hasNext) 1f else 0.5f }
    }

    fun setTitle(title: String){
        ctrView.post { ctrTitleText?.text = title }
    }

    companion object {
        fun stringForTime(timeMs: Int, sign: Boolean = false): String {
            if(sign)
                return (if(timeMs >= 0) "+" else "-") + stringForTime(Math.abs(timeMs))
            val totalSeconds = timeMs / 100
            val seconds = totalSeconds % 60
            val minutes = totalSeconds / 60 % 60
            val hours = totalSeconds / 3600

            val mStrBuilder = StringBuilder()
            val mFormatter = Formatter(mStrBuilder, Locale.getDefault())
            mStrBuilder.setLength(0)
            return if (hours > 0) {
                mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
            } else {
                mFormatter.format("%02d:%02d", minutes, seconds).toString()
            }
        }
    }
}