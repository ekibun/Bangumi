package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.net.Uri
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import com.chad.library.adapter.base.BaseQuickAdapter
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.view.*
import kotlinx.android.synthetic.main.subject_blog.view.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_character.view.*
import kotlinx.android.synthetic.main.subject_topic.view.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.github.GithubRaw
import soko.ekibun.bangumi.api.github.bean.BangumiItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.PlayerBridge
import soko.ekibun.videoplayer.bean.VideoSubject
import java.util.*

class SubjectPresenter(private val context: SubjectActivity){
    val subjectView by lazy{ SubjectView(context) }

    lateinit var subject: Subject

    fun refresh(){
        refreshSubject()
        refreshProgress(subject)
    }

    @SuppressLint("SetTextI18n")
    fun init(subject: Subject){
        this.subject = subject
        subjectView.updateSubject(subject)

        subjectView.detail.character_detail.setOnClickListener {
            WebActivity.launchUrl(context, "${subject.url}/characters")
        }

        subjectView.detail.item_progress_edit.setOnClickListener {
            EditProgressDialog.showDialog(context, this.subject, context.formhash, context.ua){
                refresh()
            }
        }

        context.title_expand.setOnClickListener {
            if(subjectView.scroll2Top()) return@setOnClickListener
            WebActivity.launchUrl(context, subject.url)
        }
        context.title_collapse.setOnClickListener {
            if(subjectView.scroll2Top()) return@setOnClickListener
            WebActivity.launchUrl(context, subject.url)
        }

        subjectView.detail.topic_detail.setOnClickListener{
            WebActivity.launchUrl(context, "${subject.url}/board")
        }

        subjectView.detail.blog_detail.setOnClickListener{
            WebActivity.launchUrl(context, "${subject.url}/reviews")
        }

        context.item_play.setOnClickListener {
            if(PlayerBridge.checkActivity(context))
                PlayerBridge.startActivity(context, VideoSubject(this.subject, context.ua))
        }

        subjectView.episodeAdapter.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, _, position ->
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let{ openEpisode(it, subject, eps) }
            true
        }
        subjectView.episodeAdapter.setOnItemClickListener { _, view, position ->
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let { if(eps.last().type != Episode.TYPE_MUSIC) {
                val epStatus = context.resources.getStringArray(R.array.episode_status)

                val popupMenu = PopupMenu(context, view)
                epStatus.forEachIndexed { index, s ->
                    popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
                popupMenu.setOnMenuItemClickListener {menu->
                    val index = menu.itemId - Menu.FIRST
                    updateProgress(subject,  if (index == 1)eps else listOf(eps.last()), if (index == 1) EpisodeDialog.WATCH_TO else SubjectProgress.EpisodeProgress.EpisodeStatus.types[if (index > 1) index - 1 else index] )
                    false
                }
                popupMenu.show()
            } else { openEpisode(it, subject, eps) } }
            //subjectView.episodeAdapter.data[position]?.let{ WebActivity.launchUrl(context, it.url) }
        }

        context.item_edit_ep.setOnClickListener {
            val eps = subjectView.episodeDetailAdapter.data.filter { it.isSelected }
            if(eps.isEmpty()) return@setOnClickListener
            val epStatus = context.resources.getStringArray(R.array.episode_status).toMutableList()
            epStatus.removeAt(1)

            val popupMenu = PopupMenu(context, context.item_edit_ep)
            epStatus.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
            popupMenu.setOnMenuItemClickListener {menu->
                val newStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.types[menu.itemId - Menu.FIRST]
                updateProgress(subject, eps.map{it.t}, newStatus)
                false
            }
            popupMenu.show()
        }
        subjectView.episodeDetailAdapter.updateSelection = {
            val eps = subjectView.episodeDetailAdapter.data.filter { it.isSelected }
            context.item_edit_ep.visibility = if(eps.isEmpty()) View.GONE else View.VISIBLE
            context.item_ep_title.visibility = context.item_edit_ep.visibility
            context.item_ep_title.text = "${context.getText(R.string.episodes)}${if(eps.isEmpty()) "" else "(${eps.size})"}"
        }

        subjectView.episodeDetailAdapter.setOnItemChildLongClickListener { _, _, position ->
            val eps = subjectView.episodeDetailAdapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            if(eps.last().type == Episode.TYPE_MUSIC)
                subjectView.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject, eps)
                    true }?:false
            else subjectView.episodeDetailAdapter.longClickListener(position)
        }

        subjectView.episodeDetailAdapter.setOnItemChildClickListener { _, _, position ->
            val eps = subjectView.episodeDetailAdapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            if(eps.last().type == Episode.TYPE_MUSIC || subjectView.episodeDetailAdapter.clickListener(position))
                subjectView.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject, eps) }
        }

        subjectView.linkedSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.linkedSubjectsAdapter.data[position]?.let{ SubjectActivity.startActivity(context, it) }
        }

        subjectView.commendSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.commendSubjectsAdapter.data[position]?.let{ SubjectActivity.startActivity(context, it) }
        }

        subjectView.characterAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.characterAdapter.data[position]?.url, "")
        }

        subjectView.topicAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.topicAdapter.data[position]?.url, "")
        }

        subjectView.blogAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.blogAdapter.data[position]?.url, "")
        }

        subjectView.sitesAdapter.setOnItemClickListener { _, _, position ->
            try{
                CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(subjectView.sitesAdapter.data[position].parseUrl()))
            }catch (e: Exception){ e.printStackTrace() }
        }

        subjectView.commentAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.commentAdapter.data[position].user?.url)
        }

        subjectView.seasonAdapter.setOnItemClickListener { _, _, position ->
            val item = subjectView.seasonAdapter.data[position]
            if(item.id != subjectView.seasonAdapter.currentId)
            SubjectActivity.startActivity(context, Subject(item.subject_id, "${Bangumi.SERVER}/subject/${item.subject_id}", subject.type, item.name, item.name_cn,
                    images = Images(item.image?.replace("/g/", "/l/"),
                            item.image?.replace("/g/", "/c/"),
                            item.image?.replace("/g/", "/m/"),
                            item.image?.replace("/g/", "/s/"),
                            item.image)))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun openEpisode(episode: Episode, subject: Subject, eps: List<Episode>){
        EpisodeDialog.showDialog(context, episode, eps, onAirInfo){mEps, status ->
            updateProgress(subject, mEps, status)
        }
    }

    private fun updateProgress(subject: Subject, eps: List<Episode>, newStatus: String){
        EpisodeDialog.updateProgress(context, eps, newStatus, context.formhash, context.ua) {
            subjectView.episodeAdapter.notifyDataSetChanged()
            subjectView.episodeDetailAdapter.notifyDataSetChanged()
            refreshProgress(subject)
        }
    }

    private var subjectCall : Call<Subject>? = null
    private fun refreshSubject(){
        subjectCall?.cancel()
        subjectCall = Bangumi.getSubject(subject, context.ua)
        subjectCall?.enqueue(ApiHelper.buildCallback({
            subject = it
            refreshLines(it)
            refreshCollection()
            subjectView.updateSubject(it)
        }, {}))

        BgmIpViewer.createInstance().subject(subject.id).enqueue(ApiHelper.buildCallback({
            val ret = BgmIpViewer.getSeason(it, subject)
            if(ret.size > 1){
                subjectView.seasonAdapter.setNewData(ret.distinct())
                subjectView.seasonAdapter.currentId = subject.id
                subjectView.seasonLayoutManager.scrollToPositionWithOffset(subjectView.seasonAdapter.data.indexOfFirst { it.subject_id == subject.id }, 0)
            }
        }, {}))

        var commentPage = 1
        subjectView.commentAdapter.setEnableLoadMore(true)
        subjectView.commentAdapter.setOnLoadMoreListener({
            loadComment(subject, commentPage)
            commentPage++
        }, context.comment_list)
        loadComment(subject, commentPage)
        commentPage++
    }

    private fun loadComment(subject: Subject, page: Int){
        Bangumi.getComments(subject, page, context.ua).enqueue(ApiHelper.buildCallback({
            if(page == 1)
                subjectView.commentAdapter.setNewData(null)
            if(it?.isEmpty() == true)
                subjectView.commentAdapter.loadMoreEnd()
            else{
                subjectView.commentAdapter.loadMoreComplete()
                subjectView.commentAdapter.addData(it)
            }
        }, {
            subjectView.detail.item_comment_header.visibility = View.VISIBLE
            subjectView.commentAdapter.loadMoreFail()}))
    }

    private var onAirInfo: OnAirInfo? = null
    private fun refreshLines(subject: Subject){
        val dateList = subject.air_date?.replace("/", "-")?.replace("年", "-")?.replace("月", "-")?.replace("日", "")?.split("-") ?: return
        val year = dateList.getOrNull(0)?.toIntOrNull()?:0
        val month = dateList.getOrNull(1)?.toIntOrNull()?:1
        GithubRaw.createInstance().bangumiData(year, String.format("%02d", month)).enqueue(ApiHelper.buildCallback({
            subjectView.sitesAdapter.setNewData(null)
            it.filter { it.sites?.filter { it.site == "bangumi" }?.getOrNull(0)?.id?.toIntOrNull() == subject.id }.forEach {
                if(subjectView.sitesAdapter.data.size == 0 && !it.officialSite.isNullOrEmpty())
                    subjectView.sitesAdapter.addData(BangumiItem.SitesBean("official", "", it.officialSite))
                subjectView.sitesAdapter.addData(it.sites?.filter { it.site != "bangumi" }?:ArrayList())
            }
            subjectView.detail.site_list.visibility = if(subjectView.sitesAdapter.data.isEmpty()) View.GONE else View.VISIBLE
        }, {}))

        GithubRaw.createInstance().onAirInfo(subject.id/1000, subject.id).enqueue(ApiHelper.buildCallback({
            onAirInfo = it
        }, {}))
    }

    private fun removeCollection(subject: Subject){
        if(context.isFinishing) return
        AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/${subject.id}/remove?gh=${context.formhash}", mapOf("User-Agent" to context.ua)){ it.code() == 200 }
                            .enqueue(ApiHelper.buildCallback({
                                if(it) subject.interest = Collection()
                                refreshCollection()
                            }, {}))
                }.show()
    }

    private fun refreshCollection(){
        val body = subject.interest?:Collection()
        val status = body.status
        context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                if(status?.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
        context.item_collect_info.text = status?.name?:context.getString(R.string.collect)

        context.item_collect.setOnClickListener{
            if(context.formhash.isEmpty()) return@setOnClickListener
            val popupMenu = PopupMenu(context, context.item_collect)
            val statusList = context.resources.getStringArray(CollectionStatusType.getTypeNamesResId(subject.type))
            statusList.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
            if(status != null)
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + statusList.size, statusList.size, R.string.delete)
            popupMenu.setOnMenuItemClickListener {menu->
                if(menu.itemId == Menu.FIRST + statusList.size){
                    removeCollection(subject)
                    return@setOnMenuItemClickListener false
                }
                val newStatus = CollectionStatusType.status[menu.itemId - Menu.FIRST]
                val newTags = if(body.tag?.isNotEmpty() == true) body.tag.reduce { acc, s -> "$acc $s" } else ""
                Bangumi.updateCollectionStatus(subject, context.formhash, context.ua,
                        newStatus, newTags, body.comment?:"", body.rating, body.private).enqueue(ApiHelper.buildCallback({
                    subject.interest = it
                    refreshCollection()
                },{}))
                false
            }
            popupMenu.show()
        }

        context.item_collect.setOnLongClickListener {
            EditSubjectDialog.showDialog(context, subject, body, context.formhash, context.ua){
                refreshCollection()
            }
            true
        }
    }

    private var epCalls: Call<List<Episode>>? = null
    private fun refreshProgress(subject: Subject){
        epCalls?.cancel()
        epCalls = Bangumi.getSubjectEps(subject.id, context.ua)
        epCalls?.enqueue(ApiHelper.buildCallback({
            subject.eps = it
            subjectView.updateEpisode(it)
        }, {}))
    }
}