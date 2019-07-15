package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.api.github.bean.BangumiItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.ui.web.WebActivity

class EpisodeDialog(context: Context): Dialog(context, R.style.AppTheme_Dialog_Floating) {
    companion object {
        const val WATCH_TO = "watch_to"
        fun showDialog(context: Context, episode: Episode, eps: List<Episode>, onAirInfo: OnAirInfo?, callback: (eps: List<Episode>, status: String)->Unit): EpisodeDialog{
            val dialog = EpisodeDialog(context)
            dialog.eps = eps
            dialog.episode = episode
            dialog.onAirInfo = onAirInfo
            dialog.callback = callback
            dialog.show()
            return dialog
        }

        fun updateProgress(context: Context, eps: List<Episode>, newStatus: String, formhash: String, ua: String, callback: (Boolean)->Unit){
            if(newStatus == WATCH_TO){
                val epIds = eps.map{ it.id.toString()}.reduce { acc, s -> "$acc,$s" }
                Bangumi.updateProgress(eps.last().id, SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, formhash, ua, epIds).enqueue(
                        ApiHelper.buildCallback({
                            val epStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.getStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH)
                            eps.forEach { it.progress = if(epStatus != null) SubjectProgress.EpisodeProgress(it.id, epStatus) else null }
                            callback(true)
                        }, { if(it != null) callback(false)}))
                return
            }
            eps.forEach {episode->
                Bangumi.updateProgress(episode.id, newStatus, formhash, ua).enqueue(
                        ApiHelper.buildCallback({
                            val epStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.getStatus(newStatus)
                            episode.progress = if(epStatus != null) SubjectProgress.EpisodeProgress(episode.id, epStatus) else null
                            callback(true)
                        }, { if(it != null) callback(false)}))
            }
        }
    }

    var eps: List<Episode> = ArrayList()
    var episode: Episode? = null
    var onAirInfo: OnAirInfo? = null
    var callback: ((eps: List<Episode>, status: String)->Unit)? = null
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_epsode, null)
        setContentView(view)
        val episode = episode?:return
        view.item_episode_title.text = episode.parseSort(context) + " " + if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else  context.getString(R.string.phrase_air_date, episode.airdate) + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else context.getString(R.string.phrase_duration, episode.duration) + "\n") +
                context.getString(R.string.phrase_comment, episode.comment)
        view.item_episode_title.setOnClickListener {
            WebActivity.launchUrl(context, "${Bangumi.SERVER}/m/topic/ep/${episode.id}", "")
        }
        val adapter = SitesAdapter(onAirInfo?.eps?.firstOrNull { it.id == episode.id }?.sites?.map{ BangumiItem.SitesBean(it.site, it.title, it.url)}?.toMutableList())
        val emptyTextView = TextView(context)
        val dp4 = (context.resources.displayMetrics.density * 4 + 0.5f).toInt()
        emptyTextView.setPadding(dp4,dp4,dp4,dp4)
        emptyTextView.setText(R.string.hint_no_play_source)
        adapter.emptyView = emptyTextView
        adapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, adapter.data[position].url, "")
        }
        view.item_site_list.adapter = adapter
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        linearLayoutManager.orientation = RecyclerView.HORIZONTAL
        view.item_site_list.layoutManager = linearLayoutManager
        when(episode.progress?.status?.id?:0){
            1 -> view.radio_queue.isChecked = true
            2 -> view.radio_watch.isChecked = true
            3 -> view.radio_drop.isChecked = true
            else -> view.radio_remove.isChecked = true
        }
        //view.item_episode_status.setSelection(intArrayOf(4,2,0,3)[episode.progress?.status?.id?:0])
        if(episode.type != Episode.TYPE_MUSIC) {
            view.item_episode_status.setOnCheckedChangeListener { _, checkedId ->
                callback?.invoke(if(checkedId == R.id.radio_watch_to)eps else listOf(episode), when(checkedId){
                    R.id.radio_watch_to -> WATCH_TO
                    R.id.radio_watch -> SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH
                    R.id.radio_queue -> SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE
                    R.id.radio_drop -> SubjectProgress.EpisodeProgress.EpisodeStatus.DROP
                    else -> SubjectProgress.EpisodeProgress.EpisodeStatus.REMOVE
                })
            }
        }else {
            view.item_episode_status.visibility = View.GONE
        }

        window?.setGravity(Gravity.BOTTOM)
        window?.attributes?.let{
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        setCanceledOnTouchOutside(true)
    }
}
