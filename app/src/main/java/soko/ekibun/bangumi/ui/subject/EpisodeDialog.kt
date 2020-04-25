package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.base_dialog.view.*
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper.subscribeOnUiThread
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 剧集对话框
 * @property eps List<Episode>
 * @property episode Episode?
 * @property callback Function2<[@kotlin.ParameterName] List<Episode>, [@kotlin.ParameterName] String, Unit>?
 * @property adapter SitesAdapter
 * @property info OnAirInfo?
 * @property title String
 */
class EpisodeDialog : BaseDialog(R.layout.base_dialog) {
    companion object {
        const val WATCH_TO = "watch_to"
        /**
         * 显示对话框
         * @param fragmentManager FragmentManager
         * @param episode Episode
         * @param eps List<Episode>
         * @param onAirInfo OnAirInfo?
         * @param callback Function2<[@kotlin.ParameterName] List<Episode>, [@kotlin.ParameterName] String, Unit>
         * @return EpisodeDialog
         */
        fun showDialog(
            fragmentManager: FragmentManager,
            episode: Episode,
            eps: List<Episode>,
            onAirInfo: OnAirInfo?,
            callback: (eps: List<Episode>, status: String) -> Unit
        ): EpisodeDialog {
            val dialog = EpisodeDialog()
            dialog.eps = eps
            dialog.episode = episode
            dialog.info = onAirInfo
            dialog.callback = callback
            dialog.show(fragmentManager, "episode")
            return dialog
        }

        /**
         * 更新进度
         * @param eps List<Episode>
         * @param newStatus String
         * @param callback Function1<Boolean, Unit>
         */
        fun updateProgress(eps: List<Episode>, newStatus: String, callback: (Boolean) -> Unit) {
            if (newStatus == WATCH_TO) {
                val epIds = eps.map { it.id.toString() }.reduce { acc, s -> "$acc,$s" }
                Subject.updateProgress(eps.last().id, Episode.PROGRESS_WATCH, epIds).subscribeOnUiThread({
                    eps.forEach { it.progress = Episode.PROGRESS_WATCH }
                    callback(true)
                }, { callback(false) })
                return
            }
            eps.forEach { episode ->
                Subject.updateProgress(episode.id, newStatus).subscribeOnUiThread({
                    episode.progress = newStatus
                    callback(true)
                }, { callback(false) })
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
    override val title: String get() = episode?.parseSort() + " " + if (episode?.name_cn.isNullOrEmpty()) episode?.name else episode?.name_cn

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        view.dialog_title.setOnClickListener {
            WebActivity.launchUrl(view.context, "${Bangumi.SERVER}/m/topic/ep/${episode?.id}", "")
        }
        LayoutInflater.from(view.context).inflate(R.layout.dialog_epsode, view.layout_content)
        val episode = episode ?: return
        view.item_episode_desc.text = (if (episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if (episode.airdate.isNullOrEmpty()) "" else view.context.getString(
                    R.string.phrase_air_date,
                    episode.airdate
                ) + "\n") +
                (if (episode.duration.isNullOrEmpty()) "" else view.context.getString(
                    R.string.phrase_duration,
                    episode.duration
                ) + "\n") +
                view.context.getString(R.string.phrase_comment, episode.comment)
        val emptyTextView = TextView(context)
        val dp4 = (view.context.resources.displayMetrics.density * 4 + 0.5f).toInt()
        emptyTextView.setPadding(dp4, dp4, dp4, dp4)
        emptyTextView.setText(R.string.hint_no_play_source)
        adapter.emptyView = emptyTextView
        adapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(view.context, adapter.data[position].url(), "")
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
