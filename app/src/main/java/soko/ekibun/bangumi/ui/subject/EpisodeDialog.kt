package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 剧集对话框
 */
class EpisodeDialog(context: Context) : BaseDialog(context, R.layout.dialog_epsode) {
    companion object {
        const val WATCH_TO = "watch_to"
        /**
         * 显示对话框
         */
        fun showDialog(
            context: Context,
            episode: Episode,
            eps: List<Episode>,
            onAirInfo: OnAirInfo?,
            callback: (eps: List<Episode>, status: String) -> Unit
        ): EpisodeDialog {
            val dialog = EpisodeDialog(context)
            dialog.eps = eps
            dialog.episode = episode
            dialog.info = onAirInfo
            dialog.callback = callback
            dialog.show()
            return dialog
        }

        /**
         * 更新进度
         */
        fun updateProgress(eps: List<Episode>, newStatus: String, callback: (Boolean) -> Unit) {
            if (newStatus == WATCH_TO) {
                val epIds = eps.map { it.id.toString() }.reduce { acc, s -> "$acc,$s" }
                Subject.updateProgress(eps.last().id, Episode.PROGRESS_WATCH, epIds).enqueue(
                        ApiHelper.buildCallback({
                            eps.forEach { it.progress = Episode.PROGRESS_WATCH }
                            callback(true)
                        }, { if (it != null) callback(false) }))
                return
            }
            eps.forEach { episode ->
                Subject.updateProgress(episode.id, newStatus).enqueue(
                        ApiHelper.buildCallback({
                            episode.progress = newStatus
                            callback(true)
                        }, { if (it != null) callback(false) })
                )
            }
        }
    }

    var eps: List<Episode> = ArrayList()
    var episode: Episode? = null
    var callback: ((eps: List<Episode>, status: String) -> Unit)? = null
    val adapter = SitesAdapter()
    var info: OnAirInfo? = null
        set(value) {
            field = value
            adapter.setNewData(value?.eps?.find { it.id == episode?.id }?.sites)
        }
    override val title: String get() = episode?.parseSort(context) + " " + if (episode?.name_cn.isNullOrEmpty()) episode?.name else episode?.name_cn
    override fun onTitleClick() {
        WebActivity.launchUrl(context, "${Bangumi.SERVER}/m/topic/ep/${episode?.id}", "")
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        val episode = episode ?: return
        view.item_episode_desc.text = (if (episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if (episode.airdate.isNullOrEmpty()) "" else context.getString(
                    R.string.phrase_air_date,
                    episode.airdate
                ) + "\n") +
                (if (episode.duration.isNullOrEmpty()) "" else context.getString(
                    R.string.phrase_duration,
                    episode.duration
                ) + "\n") +
                context.getString(R.string.phrase_comment, episode.comment)
        val emptyTextView = TextView(context)
        val dp4 = (context.resources.displayMetrics.density * 4 + 0.5f).toInt()
        emptyTextView.setPadding(dp4, dp4, dp4, dp4)
        emptyTextView.setText(R.string.hint_no_play_source)
        adapter.emptyView = emptyTextView
        adapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, adapter.data[position].url(), "")
        }
        view.item_site_list.adapter = adapter
        view.post { info = info }
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        linearLayoutManager.orientation = RecyclerView.HORIZONTAL
        view.item_site_list.layoutManager = linearLayoutManager
        when (episode.progress) {
            Episode.PROGRESS_QUEUE -> view.radio_queue.isChecked = true
            Episode.PROGRESS_WATCH -> view.radio_watch.isChecked = true
            Episode.PROGRESS_DROP -> view.radio_drop.isChecked = true
            else -> view.radio_remove.isChecked = true
        }

        if (episode.type != Episode.TYPE_MUSIC) {
            view.item_episode_status.setOnCheckedChangeListener { _, checkedId ->
                callback?.invoke(if (checkedId == R.id.radio_watch_to) eps else listOf(episode), when (checkedId) {
                    R.id.radio_watch_to -> WATCH_TO
                    R.id.radio_watch -> Episode.PROGRESS_WATCH
                    R.id.radio_queue -> Episode.PROGRESS_QUEUE
                    R.id.radio_drop -> Episode.PROGRESS_DROP
                    else -> Episode.PROGRESS_REMOVE
                })
            }
        } else {
            view.item_episode_status.visibility = View.GONE
        }

        val paddingBottom = view.item_site_list.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_site_list.setPadding(
                view.item_site_list.paddingLeft,
                view.item_site_list.paddingTop,
                view.item_site_list.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }
}
