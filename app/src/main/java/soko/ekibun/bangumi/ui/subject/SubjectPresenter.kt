package soko.ekibun.bangumi.ui.subject

import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.dialog_subject.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Comment
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 条目Presenter
 * @property context SubjectActivity
 * @property subject Subject
 * @property subjectView SubjectView
 * @property dataCacheModel DataCacheModel
 * @property commentPage Int
 * @property episodeDialog EpisodeDialog?
 * @property subjectRefreshListener Function1<Any?, Unit>
 * @constructor
 */
class SubjectPresenter(private val context: SubjectActivity, var subject: Subject) {
    val subjectView by lazy { SubjectView(context) }

    private val dataCacheModel by lazy { App.app.dataCacheModel }

    var commentPage = 1

    init {
        DataCacheModel.merge(subject, dataCacheModel.get(subject.cacheKey))
        subjectView.updateSubject(subject)

        subjectView.detail.item_subject_info.setOnClickListener {
            InfoboxDialog.showDialog(context.supportFragmentManager, subject)
        }
        subjectView.detail.item_detail.setOnClickListener {
            InfoboxDialog.showDialog(context.supportFragmentManager, subject)
        }
        subjectView.detail.item_friend_score_label.setOnClickListener {
            WebActivity.launchUrl(context, "http://bgm.tv/subject/${subject.id}/collections?filter=friends", "")
        }
        subjectView.detail.detail_friend_score.setOnClickListener {
            WebActivity.launchUrl(context, "http://bgm.tv/subject/${subject.id}/collections?filter=friends", "")
        }
        subjectView.sitesAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.sitesAdapter.data[position].url(), "")
        }

        subjectView.commentAdapter.data.add(0, Comment())
        subjectView.commentAdapter.loadMoreModule.setOnLoadMoreListener { loadComment(subject) }
        loadComment(subject)

        context.item_swipe.setOnRefreshListener {
            refresh()
        }

        subjectView.detail.character_detail.setOnClickListener {
            WebActivity.startActivity(context, "${subject.url}/characters")
        }

        subjectView.detail.item_progress_edit.setOnClickListener {
            updateSubjectProgress(subjectView.detail.item_vol_status.value, subjectView.detail.item_ep_status.value)
        }
        val updateInt = { _: Int ->
            subjectView.detail.item_progress_edit.visibility =
                if (subjectView.detail.item_ep_status.value == this.subject.ep_status
                    && subjectView.detail.item_vol_status.value == this.subject.vol_status
                )
                    View.INVISIBLE else View.VISIBLE
        }
        subjectView.detail.item_vol_status.setValueChangedListener(updateInt)
        subjectView.detail.item_ep_status.setValueChangedListener(updateInt)

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

        subjectView.episodeAdapter.setOnItemLongClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.episodeAdapter.data[position].url, "")
            true
        }

        subjectView.episodeAdapter.setOnItemClickListener { _, _, position ->
            showEpisodeDialog(subjectView.episodeAdapter.data[position].id)
        }

        subjectView.detail.episode_detail.setOnClickListener {
            showEpisodeListDialog()
        }

        subjectView.linkedSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.linkedSubjectsAdapter.data[position].let { SubjectActivity.startActivity(context, it) }
        }

        subjectView.recommendSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.recommendSubjectsAdapter.data[position].let { SubjectActivity.startActivity(context, it) }
        }

        subjectView.characterAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.characterAdapter.data[position].url, "")
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

    fun updateHistory() {
        context.subscribe { HistoryModel.addHistory(subject) }
    }

    /**
     * 显示剧集列表对话框
     */
    fun showEpisodeListDialog() {
        EpisodeListDialog.showDialog(context.supportFragmentManager, this)
    }

    private var episodeDialog: EpisodeDialog? = null

    /**
     * 显示剧集信息
     * @param id Int
     */
    fun showEpisodeDialog(id: Int) {
        val eps = subjectView.episodeDetailAdapter.data.mapNotNull { it.t }
        val episodeIndex = eps.indexOfFirst { it.id == id }
        val episode = eps.getOrNull(episodeIndex) ?: return
        episodeDialog = EpisodeDialog.showDialog(
            context.supportFragmentManager,
            episode,
            eps.subList(0, episodeIndex + 1).filter { it.progress != Episode.PROGRESS_DROP },
            subject.onair
        ) { mEps, status, onComplete ->
            updateProgress(mEps, status, onComplete)
        }
    }

    /**
     * 更新进度
     * @param eps List<Episode>
     * @param newStatus String
     */
    fun updateProgress(eps: List<Episode>, newStatus: String, _callback: (Boolean) -> Unit) {
        val callback = { ret: Boolean ->
            subjectView.episodeAdapter.notifyDataSetChanged()
            subjectView.episodeDetailAdapter.notifyDataSetChanged()
            refreshProgress()
            _callback(ret)
        }
        if (newStatus == EpisodeDialog.WATCH_TO) {
            val epIds = eps.map { it.id.toString() }.reduce { acc, s -> "$acc,$s" }
            context.subscribe({
                callback(false)
            }) {
                Subject.updateProgress(eps.last().id, Episode.PROGRESS_WATCH, epIds)
                eps.forEach { it.progress = Episode.PROGRESS_WATCH }
                callback(true)
            }
            return
        }
        eps.forEach { episode ->
            context.subscribe({
                callback(false)
            }) {
                Subject.updateProgress(episode.id, newStatus)
                episode.progress = newStatus
                callback(true)
            }
        }
    }

    fun updateSubjectProgress(vol: Int?, ep: Int?) {
        context.subscribe {
            Subject.updateSubjectProgress(
                subject,
                watchedeps = (ep ?: subject.ep_status).toString(),
                watched_vols = (vol ?: subject.vol_status).toString()
            )
            refresh()
        }
    }

    var subjectRefreshListener = { _: Any? -> }
    /**
     * 刷新
     */
    fun refresh() {
        context.item_swipe.isRefreshing = true

        // 不存在会卡很久，单独作为一个请求
        context.subscribe(key = "bangumi_subject_onair") {
            val airInfo = Github.onAirInfo(subject.id) ?: return@subscribe
            subject.onair = airInfo
            subjectView.updateSubject(subject, Subject.SaxTag.ONAIR)
            dataCacheModel.set(subject.cacheKey, subject)
            episodeDialog?.info = airInfo
        }

        context.subscribe(onComplete = {
            context.item_swipe.isRefreshing = false
        }, key = "bangumi_subject_detail") {
            coroutineScope {
                listOf(
                    async {
                        Subject.getDetail(subject) {
                            if (it in arrayOf(Subject.SaxTag.COLLECT, Subject.SaxTag.NONE)) refreshCollection()
                            subjectView.updateSubject(subject, if (it == Subject.SaxTag.NONE) null else it)
                        }
                        subjectView.updateSubject(subject)
                        subjectRefreshListener(subject)
                        dataCacheModel.set(subject.cacheKey, subject)
                    },
                    async {
                        subject.season = Github.getSeason(subject.id)
                        subjectView.updateSubject(subject, Subject.SaxTag.SEASON)
                    },
                    async {
                        val eps = subjectView.updateEpisode(Episode.getSubjectEps(subject))
                        subjectView.updateEpisodeLabel(eps, subject)
                        subject.eps = eps
                    }
                ).awaitAll()
            }
        }
    }

    private fun loadComment(subject: Subject) {
        context.subscribe({
            subjectView.commentAdapter.loadMoreModule.loadMoreFail()
        }, key = "bangumi_subject_comment") {
            val comment = Comment.getSubjectComment(subject, commentPage)
            commentPage++
            subjectView.commentAdapter.addData(comment)
            if (comment.isEmpty()) {
                subjectView.commentAdapter.loadMoreModule.loadMoreEnd()
            } else {
                subjectView.commentAdapter.loadMoreModule.loadMoreComplete()
            }
        }
    }

    private fun removeCollection(subject: Subject) {
        if (context.isFinishing) return
        AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                context.subscribe {
                    Collection.remove(subject)
                    subject.collect = Collection()
                    refreshCollection()
                }
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
        subjectView.tagAdapter.setNewInstance(subjectView.tagAdapter.data)

        subjectView.detail.item_collect.setOnClickListener {
            if (HttpUtil.formhash.isEmpty()) return@setOnClickListener
            val popupMenu = PopupMenu(context, subjectView.detail.item_collect)
            val statusList = context.resources.getStringArray(Collection.getStatusNamesRes(subject.type))
            statusList.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, index, index, s)
            }
            if (status != null) {
                popupMenu.menu.add(Menu.NONE, statusList.size, statusList.size, R.string.delete)
                ResourceUtil.checkMenu(context, popupMenu.menu) {
                    it.itemId + 1 == subject.collect?.statusId
                }
            }

            popupMenu.setOnMenuItemClickListener { menu ->
                if (menu.itemId == statusList.size) {
                    removeCollection(subject)
                    return@setOnMenuItemClickListener true
                }
                context.subscribe {
                    val collection = Collection(
                        status = Collection.getStatusById(menu.itemId + 1),
                        rating = body.rating,
                        comment = body.comment,
                        private = body.private,
                        tag = body.tag
                    )
                    Collection.updateStatus(subject, collection)
                    subject.collect = collection
                    refreshCollection()
                }
                true
            }
            popupMenu.show()
        }

        subjectView.detail.item_collect.setOnLongClickListener {
            EditSubjectDialog.showDialog(context.supportFragmentManager, subject, body) {
                refreshCollection()
            }
            true
        }
    }

    private fun refreshProgress() {
        context.subscribe(key = "bangumi_subject_progress") {
            val eps = subjectView.updateEpisode(Episode.getSubjectEps(subject))
            subjectView.updateEpisodeLabel(eps, subject)
            subject.eps = eps
            dataCacheModel.set(subject.cacheKey, subject)
            subjectRefreshListener(eps)
        }
    }
}