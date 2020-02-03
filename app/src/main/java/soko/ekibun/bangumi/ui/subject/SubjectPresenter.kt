package soko.ekibun.bangumi.ui.subject

import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import com.chad.library.adapter.base.BaseQuickAdapter
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.brvah_quick_view_load_more.view.*
import kotlinx.android.synthetic.main.dialog_subject.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import retrofit2.Call
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Comment
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.Jsdelivr
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.bangumi.api.trim21.bean.IpView
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 条目Presenter
 */
class SubjectPresenter(private val context: SubjectActivity, var subject: Subject) {
    val subjectView by lazy { SubjectView(context) }

    private val dataCacheModel by lazy { App.get(context).dataCacheModel }

    var commentPage = 1

    /**
     * 初始化
     */
    init {
        DataCacheModel.merge(subject, dataCacheModel.get(subject.cacheKey))
        subjectView.updateSubject(subject)

        subjectView.detail.item_subject_info.setOnClickListener {
            InfoboxDialog.showDialog(context, subject)
        }
        subjectView.detail.item_detail.setOnClickListener {
            InfoboxDialog.showDialog(context, subject)
        }
        subjectView.sitesAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.sitesAdapter.data[position].url(), "")
        }

        subjectView.commentAdapter.setEnableLoadMore(true)
        subjectView.commentAdapter.setLoadMoreView(BrvahLoadMoreView())
        subjectView.commentAdapter.setOnLoadMoreListener({
            loadComment(subject)
        }, context.item_list)
        loadComment(subject)
        subjectView.detail.load_more_load_fail_view.setOnClickListener {
            loadComment(subject)
        }

        context.item_swipe.setOnRefreshListener {
            refresh()
        }

        subjectView.detail.character_detail.setOnClickListener {
            WebActivity.startActivity(context, "${subject.url}/characters")
        }

        subjectView.detail.item_progress_edit.setOnClickListener {
            Subject.updateSubjectProgress(
                subject,
                watchedeps = subjectView.detail.item_ep_status.value.toString(),
                watched_vols = subjectView.detail.item_vol_status.value.toString()
            )
                .enqueue(ApiHelper.buildCallback({
                    refresh()
                }, {}))
        }
        val updateInt = { _: Int ->
            subjectView.detail.item_progress_edit.visibility =
                if (subjectView.detail.item_ep_status.value == this.subject.ep_status
                    && subjectView.detail.item_vol_status.value == this.subject.vol_status
                )
                    View.INVISIBLE else View.VISIBLE
        }
        subjectView.detail.item_vol_status.setListener(updateInt)
        subjectView.detail.item_ep_status.setListener(updateInt)

        subjectView.collapsibleAppBarHelper.onTitleClickListener = onTitleClickListener@{
            if (subjectView.scroll2Top()) return@onTitleClickListener
            WebActivity.startActivity(context, subject.url)
        }

        subjectView.detail.topic_detail.setOnClickListener {
            WebActivity.startActivity(context, "${subject.url}/board")
        }

        subjectView.detail.blog_detail.setOnClickListener {
            WebActivity.startActivity(context, "${subject.url}/reviews")
        }

        subjectView.episodeAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { _, _, position ->
                WebActivity.launchUrl(context, subjectView.episodeAdapter.data[position].url, "")
                true
            }

        subjectView.episodeAdapter.setOnItemClickListener { _, _, position ->
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let { openEpisode(it, eps) }
        }

        subjectView.detail.episode_detail.setOnClickListener {
            EpisodeListDialog.showDialog(context, this)
        }

        subjectView.linkedSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.linkedSubjectsAdapter.data[position]?.let { SubjectActivity.startActivity(context, it) }
        }

        subjectView.recommendSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.recommendSubjectsAdapter.data[position]?.let { SubjectActivity.startActivity(context, it) }
        }

        subjectView.characterAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.characterAdapter.data[position]?.url, "")
        }

        subjectView.topicAdapter.setOnItemClickListener { _, _, position ->
            TopicActivity.startActivity(context, subjectView.topicAdapter.data[position])
        }

        subjectView.blogAdapter.setOnItemClickListener { _, _, position ->
            TopicActivity.startActivity(context, subjectView.blogAdapter.data[position])
        }

        subjectView.commentAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.startActivity(context, subjectView.commentAdapter.data[position].user?.url)
        }

        subjectView.seasonAdapter.setOnItemClickListener { _, _, position ->
            val item = subjectView.seasonAdapter.data[position]
            if (item.id != subjectView.seasonAdapter.currentId)
                SubjectActivity.startActivity(context, subjectView.seasonAdapter.data[position])
        }
    }

    private var episodeDialog: EpisodeDialog? = null
    fun openEpisode(episode: Episode, eps: List<Episode>) {
        episodeDialog = EpisodeDialog.showDialog(context, episode, eps, subject.onair) { mEps, status ->
            updateProgress(mEps, status)
        }
    }

    fun updateProgress(eps: List<Episode>, newStatus: String) {
        EpisodeDialog.updateProgress(eps, newStatus) {
            subjectView.episodeAdapter.notifyDataSetChanged()
            subjectView.episodeDetailAdapter.notifyDataSetChanged()
            refreshProgress()
        }
    }

    private var subjectCall: Call<Unit>? = null
    var subjectRefreshListener = { _: Any? -> }
    /**
     * 刷新
     */
    fun refresh() {
        subjectCall?.cancel()
        context.item_swipe.isRefreshing = true

        subjectCall = ApiHelper.buildGroupCall(
            arrayOf(
                Jsdelivr.createInstance().onAirInfo(subject.id / 1000, subject.id),
                Subject.getDetail(subject) { newSubject, tag ->
                    subject = newSubject
                    context.runOnUiThread {
                        if (tag == Subject.SaxTag.COLLECT) refreshCollection()
                        subjectView.updateSubject(newSubject, tag)
                    }
                },
                BgmIpViewer.createInstance().subject(subject.id),
                Episode.getSubjectEps(subject)
            )
        ) { _, it ->
            when (it) {
                is Subject -> {
                    DataCacheModel.merge(subject, it)
                    refreshCollection()
                    subjectView.updateSubject(it)
                }
                is OnAirInfo -> {
                    subject.onair = it
                    subjectView.updateSubject(subject, Subject.SaxTag.ONAIR)
                    episodeDialog?.info = it
                }
                is IpView -> {
                    val season = BgmIpViewer.getSeason(it, subject)
                    subject.season = season
                    subjectView.updateSubject(subject, Subject.SaxTag.SEASON)
                }
                is List<*> -> {
                    val eps = subjectView.updateEpisode(it.mapNotNull { it as? Episode })
                    subjectView.updateEpisodeLabel(eps, subject)
                    subject.eps = eps
                }
            }
            subjectRefreshListener(it)
            dataCacheModel.set(subject.cacheKey, subject)
        }

        subjectCall?.enqueue(ApiHelper.buildCallback({}, {
            context.item_swipe.isRefreshing = false
        }))
    }

    private fun loadComment(subject: Subject) {
        val page = commentPage
        subjectView.detail.comment_load_info.visibility = if (page == 1) View.VISIBLE else View.GONE
        subjectView.detail.load_more_loading_view.visibility = View.VISIBLE
        subjectView.detail.load_more_load_fail_view.visibility = View.GONE
        subjectView.detail.load_more_load_end_view.visibility = View.GONE

        Comment.getSubjectComment(subject, page).enqueue(ApiHelper.buildCallback({
            commentPage++
            if (page == 1)
                subjectView.commentAdapter.setNewData(null)
            if (it?.isEmpty() == true) {
                subjectView.detail.load_more_loading_view.visibility = View.GONE
                subjectView.detail.load_more_load_fail_view.visibility = View.GONE
                subjectView.detail.load_more_load_end_view.visibility = View.VISIBLE
                subjectView.commentAdapter.loadMoreEnd()
            } else {
                subjectView.detail.comment_load_info.visibility = View.GONE
                subjectView.commentAdapter.loadMoreComplete()
                subjectView.commentAdapter.addData(it)
            }
        }, {
            subjectView.detail.item_comment_header.visibility = View.VISIBLE
            subjectView.commentAdapter.loadMoreFail()

            subjectView.detail.load_more_loading_view.visibility = View.GONE
            subjectView.detail.load_more_load_fail_view.visibility = View.VISIBLE
            subjectView.detail.load_more_load_end_view.visibility = View.GONE
        }))
    }

    private fun removeCollection(subject: Subject) {
        if (context.isFinishing) return
        AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                Collection.remove(subject).enqueue(ApiHelper.buildCallback({
                    if (it) subject.collect = Collection()
                    refreshCollection()
                }, {}))
            }.show()
    }

    private fun refreshCollection() {
        val body = subject.collect ?: Collection()
        val status = body.status
        subjectView.detail.item_collect_image.setImageDrawable(
            context.resources.getDrawable(
                if (status in listOf(
                        Collection.STATUS_WISH,
                        Collection.STATUS_COLLECT,
                        Collection.STATUS_DO,
                        Collection.STATUS_ON_HOLD
                    )
                ) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme
            )
        )
        subjectView.detail.item_collect_info.text =
            context.resources.getStringArray(Collection.getStatusNamesRes(subject.type)).getOrNull(body.statusId - 1)
                ?: context.getString(R.string.collect)

        subjectView.tagAdapter.hasTag = {
            body.tag?.contains(it) ?: false
        }
        subjectView.tagAdapter.setNewData(subjectView.tagAdapter.data)

        subjectView.detail.item_collect.setOnClickListener {
            if (HttpUtil.formhash.isEmpty()) return@setOnClickListener
            val popupMenu = PopupMenu(context, subjectView.detail.item_collect)
            val statusList = context.resources.getStringArray(Collection.getStatusNamesRes(subject.type))
            statusList.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s)
            }
            if (status != null)
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + statusList.size, statusList.size, R.string.delete)
            popupMenu.setOnMenuItemClickListener { menu ->
                if (menu.itemId == Menu.FIRST + statusList.size) {
                    removeCollection(subject)
                    return@setOnMenuItemClickListener false
                }
                Collection.updateStatus(
                    subject, Collection(
                        status = Collection.getStatusById(menu.itemId - Menu.FIRST + 1),
                        rating = body.rating,
                        comment = body.comment,
                        private = body.private,
                        tag = body.tag
                    )
                ).enqueue(ApiHelper.buildCallback({
                    subject.collect = it
                    refreshCollection()
                }, {}))
                false
            }
            popupMenu.show()
        }

        subjectView.detail.item_collect.setOnLongClickListener {
            EditSubjectDialog.showDialog(context, subject, body) {
                refreshCollection()
            }
            true
        }
    }

    private var epCalls: Call<List<Episode>>? = null
    private fun refreshProgress() {
        epCalls?.cancel()
        epCalls = Episode.getSubjectEps(subject)
        epCalls?.enqueue(ApiHelper.buildCallback({
            val eps = subjectView.updateEpisode(it)
            subjectView.updateEpisodeLabel(eps, subject)
            subject.eps = eps
            dataCacheModel.set(subject.cacheKey, subject)
        }, {}))
    }
}