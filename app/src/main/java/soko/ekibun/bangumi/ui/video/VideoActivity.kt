package soko.ekibun.bangumi.ui.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_video.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil

class VideoActivity : AppCompatActivity() {
    val videoPresenter: VideoPresenter by lazy { VideoPresenter(this) }
    val systemUIPresenter: SystemUIPresenter by lazy{ SystemUIPresenter(this) }

    private val subjectPresenter: SubjectPresenter by lazy{ SubjectPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        setSupportActionBar(toolbar)
        supportActionBar?.let{
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        systemUIPresenter.init()

        registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        onMultiWindowModeChanged((Build.VERSION.SDK_INT >=24 && isInMultiWindowMode), newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        systemUIPresenter.onWindowModeChanged(isInMultiWindowMode, newConfig)
        if(video_surface_container.visibility == View.VISIBLE)
            videoPresenter.controller.doShowHide(false)
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    private val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.getIntExtra(EXTRA_CONTROL_TYPE,0)){
                CONTROL_TYPE_PAUSE->{
                    videoPresenter.doPlayPause(false)
                }
                CONTROL_TYPE_PLAY->{
                    videoPresenter.doPlayPause(true)
                }
                CONTROL_TYPE_NEXT->
                    videoPresenter.next?.let{videoPresenter.doPlay(it)}
                CONTROL_TYPE_PREV->
                    videoPresenter.prev?.let{videoPresenter.doPlay(it)}
            }
        }
    }

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(systemUIPresenter.isLandscape && videoPresenter.videoModel.player.playWhenReady && Build.VERSION.SDK_INT >= 24) {
            @Suppress("DEPRECATION") enterPictureInPictureMode()
            setPictureInPictureParams(false)
        }
    }

    fun setPictureInPictureParams(playPause: Boolean){
        if(Build.VERSION.SDK_INT >= 26) {
            val actionPrev = RemoteAction(Icon.createWithResource(this, R.drawable.ic_prev), getString(R.string.next_video), getString(R.string.next_video),
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_PREV, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
            actionPrev.isEnabled = videoPresenter.prev != null
            val actionNext = RemoteAction(Icon.createWithResource(this, R.drawable.ic_next), getString(R.string.next_video), getString(R.string.next_video),
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_NEXT, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
            actionNext.isEnabled = videoPresenter.next != null
            try{
                setPictureInPictureParams(PictureInPictureParams.Builder().setActions(listOf(
                        actionPrev,
                        RemoteAction(Icon.createWithResource(this, if (playPause) R.drawable.ic_play else R.drawable.ic_pause), getString(R.string.play_pause), getString(R.string.play_pause),
                                PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                                        if (playPause) CONTROL_TYPE_PLAY else CONTROL_TYPE_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)),
                        actionNext
                )).build())
            }catch(e: Exception){ }
        }
    }

    var pauseOnStop = false
    override fun onStart() {
        super.onStart()
        if(videoPresenter.videoModel.player.duration >0 && pauseOnStop)
            videoPresenter.doPlayPause(true)
        pauseOnStop = false
    }

    override fun onStop() {
        super.onStop()
        if(videoPresenter.videoModel.player.playWhenReady)
            pauseOnStop = true
        videoPresenter.doPlayPause(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    //back
    private fun processBack(){
        when {
            systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            episode_detail_list.visibility == View.VISIBLE -> subjectPresenter.subjectView.showEpisodeDetail(false)
            else -> finish()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_share -> {
                val subject = JsonUtil.toEntity(intent.getStringExtra(EXTRA_SUBJECT), Subject::class.java)
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share))
                intent.putExtra(Intent.EXTRA_TEXT, subject.name + " " + subject.url)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object{
        const val EXTRA_SUBJECT = "extraSubject"
        const val ACTION_MEDIA_CONTROL = "bangumiActionMediaControl"
        const val EXTRA_CONTROL_TYPE = "extraControlType"
        const val CONTROL_TYPE_PAUSE = 1
        const val CONTROL_TYPE_PLAY = 2
        const val CONTROL_TYPE_NEXT = 3
        const val CONTROL_TYPE_PREV = 4

        fun startActivity(context: Context, subject: Subject) {
            context.startActivity(parseIntent(context, subject))
        }

        private fun parseIntent(context: Context, subject: Subject): Intent {
            val intent = Intent(context, VideoActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
