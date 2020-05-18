package soko.ekibun.bangumi.ui.subject

import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.dialog_subject.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Comment
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
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
        HistoryModel.addHistory(subject)
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
    fun updateProgress(eps: List<Episode>, newStatus: String, callback: (Boolean) -> Unit) {
        if (newStatus == EpisodeDialog.WATCH_TO) {
            val epIds = eps.map { it.id.toString() }.reduce { acc, s -> "$acc,$s" }
            context.disposeContainer.subscribeOnUiThread(
                Subject.updateProgress(eps.last().id, Episode.PROGRESS_WATCH, epIds), {
                    eps.forEach { it.progress = Episode.PROGRESS_WATCH }
                    callback(true)
                }, { callback(false) }, {
                    subjectView.episodeAdapter.notifyDataSetChanged()
                    subjectView.episodeDetailAdapter.notifyDataSetChanged()
                    refreshProgress()
                })
            return
        }
        eps.forEach { episode ->
            context.disposeContainer.subscribeOnUiThread(
                Subject.updateProgress(episode.id, newStatus), {
                    episode.progress = newStatus
                    callback(true)
                }, { callback(false) }, {
                    subjectView.episodeAdapter.notifyDataSetChanged()
                    subjectView.episodeDetailAdapter.notifyDataSetChanged()
                    refreshProgress()
                })
        }
    }

    fun updateSubjectProgress(vol: Int?, ep: Int?) {
        context.disposeContainer.subscribeOnUiThread(
            Subject.updateSubjectProgress(
                subject,
                watchedeps = (ep ?: subject.ep_status).toString(),
                watched_vols = (vol ?: subject.vol_status).toString()
            ), {
                refresh()
            }
        )
    }

    var subjectRefreshListener = { _: Any? -> }
    /**
     * 刷新
     */
    fun refresh() {
        context.item_swipe.isRefreshing = true

        // 不存在会卡很久，单独作为一个请求
        context.disposeContainer.subscribeOnUiThread(
            Github.onAirInfo(subject.id).onErrorResumeNext(Observable.empty()),
            {
                subject.onair = it
                subjectView.updateSubject(subject, Subject.SaxTag.ONAIR)
                dataCacheModel.set(subject.cacheKey, subject)
                episodeDialog?.info = it
            },
            key = "bangumi_subject_onair"
        )

        context.disposeContainer.subscribeOnUiThread(
            Observable.merge(
                Subject.getDetail(subject),
                BgmIpViewer.getSeason(subject.id).onErrorResumeNext(Observable.empty()),
                Episode.getSubjectEps(subject)
            ),
            {
                when (it) {
                    is Subject.SaxTag -> {
                        if (it in arrayOf(Subject.SaxTag.COLLECT, Subject.SaxTag.NONE)) refreshCollection()
                        subjectView.updateSubject(subject, if (it == Subject.SaxTag.NONE) null else it)
                    }
                    is BgmIpViewer.SeasonData -> {
                        subject.season = it.seasons
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
            }, onComplete = {
                context.item_swipe.isRefreshing = false
            },
            key = "bangumi_subject_detail"
        )
    }

    private fun loadComment(subject: Subject) {
        val page = commentPage

        context.disposeContainer.subscribeOnUiThread(
            Comment.getSubjectComment(subject, page),
            {
                commentPage++
                subjectView.commentAdapter.addData(it)
                if (it.isEmpty()) {
                    subjectView.commentAdapter.loadMoreModule.loadMoreEnd()
                } else {
                    subjectView.commentAdapter.loadMoreModule.loadMoreComplete()
                }
            }, {
                subjectView.commentAdapter.loadMoreModule.loadMoreFail()
            },
            key = "bangumi_subject_comment"
        )
    }

    private fun removeCollection(subject: Subject) {
        if (context.isFinishing) return
        AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
            .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                context.disposeContainer.subscribeOnUiThread(
                    Collection.remove(subject),
                    {
                        if (it) subject.collect = Collection()
                        refreshCollection()
                    }
                )
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
                context.disposeContainer.subscribeOnUiThread(
                    Collection.updateStatus(
                        subject, Collection(
                            status = Collection.getStatusById(menu.itemId + 1),
                            rating = body.rating,
                            comment = body.comment,
                            private = body.private,
                            tag = body.tag
                        )
                    ),
                    {
                        subject.collect = it
                        refreshCollection()
                    }
                )
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
        context.disposeContainer.subscribeOnUiThread(
            Episode.getSubjectEps(subject),
            {
                val eps = subjectView.updateEpisode(it)
                subjectView.updateEpisodeLabel(eps, subject)
                subject.eps = eps
                dataCacheModel.set(subject.cacheKey, subject)
                subjectRefreshListener(eps)
            },
            key = "bangumi_subject_progress"
        )
    }
}