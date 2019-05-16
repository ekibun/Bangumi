package soko.ekibun.videoplayer.service

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.ui.subject.EditSubjectDialog
import soko.ekibun.bangumi.ui.subject.EpisodeDialog
import soko.ekibun.bangumi.util.PlayerBridge
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoSubject =  intent.getParcelableExtra<VideoSubject>(PlayerBridge.EXTRA_SUBJECT)
        val ua = videoSubject.getUserAgent()
        val subject = videoSubject.toSubject()

        when(intent?.action){
            "soko.ekibun.videoplayer.updateCollection.bangumi" -> {
                EditSubjectDialog.showDialog(this, subject, subject.interest?: Collection(), subject.formhash?:"", ua?:""){
                    val intent = Intent()
                    intent.putExtra(PlayerBridge.EXTRA_SUBJECT, VideoSubject(subject, ua))
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
            "soko.ekibun.videoplayer.updateProgress.bangumi" -> {
                val eps = intent.getParcelableArrayListExtra<VideoEpisode>(PlayerBridge.EXTRA_EPISODE_LIST).map { it.toEpisode() }
                val dialog = EpisodeDialog.showDialog(this, eps.last(), eps, null){ mEps, status ->
                    EpisodeDialog.updateProgress(this, mEps, status, subject.formhash?:"", ua?:"") {
                        val intent = Intent()
                        intent.putParcelableArrayListExtra(PlayerBridge.EXTRA_EPISODE_LIST, ArrayList(eps.map{ VideoEpisode(it) }))
                        setResult(RESULT_OK, intent)
                    }
                }
                dialog.setOnDismissListener {
                    finish()
                }
            }
            else -> finish()
        }
    }
}
