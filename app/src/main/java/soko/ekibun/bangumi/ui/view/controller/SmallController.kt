package soko.ekibun.bangumi.ui.view.controller

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.controller_small.view.*
import soko.ekibun.bangumi.R

class SmallController(view: ViewGroup, onClick:(Action)->Unit, onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener): Controller(R.layout.controller_small, view) {
    override val ctrLayout: View by lazy{ ctrView.ctr_layout }
    override val ctrPlayPause: ImageButton by lazy{ ctrView.ctr_pause }
    override val ctrTimeText: TextView by lazy{ ctrView.ctr_time }
    override val ctrFullscreen: ImageButton by lazy{ ctrView.ctr_fullscreen }
    override val ctrSeekBar: SeekBar by lazy{ ctrView.ctr_seek }
    override val ctrTitleText: TextView? = null
    override val ctrNext: ImageButton? = null
    override val ctrDanmaku: ImageButton? = null

    init{
        initView(view, onClick, onSeekBarChangeListener)
    }
}