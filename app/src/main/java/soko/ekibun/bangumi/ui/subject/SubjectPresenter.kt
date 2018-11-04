package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.view.*
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import kotlinx.android.synthetic.main.subject_blog.view.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_topic.view.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumiData.BangumiData
import soko.ekibun.bangumi.api.bangumiData.bean.BangumiItem
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.bangumi.api.trim21.bean.IpView
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.PlayerBridge
import java.util.*

class SubjectPresenter(private val context: SubjectActivity){
    val api by lazy { Bangumi.createInstance() }
    val subjectView by lazy{ SubjectView(context) }
    private val userModel by lazy { UserModel(context) }

    fun init(subject: Subject){
        subjectView.updateSubject(subject)
        refreshSubject(subject)
        refreshProgress(subject)
        refreshCollection(subject)

        subjectView.detail.item_detail.setOnClickListener {
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
                PlayerBridge.startActivity(context, subject, userModel.getToken())
        }

        subjectView.episodeAdapter.setOnItemLongClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{ openEpisode(it, subject) }
            true
        }

        subjectView.episodeAdapter.setOnItemClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{ WebActivity.launchUrl(context, it.url) }
        }

        subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            subjectView.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject) }
            true
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            subjectView.episodeDetailAdapter.data[position]?.t?.let{ WebActivity.launchUrl(context, it.url) }
        }

        subjectView.linkedSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.linkedSubjectsAdapter.data[position]?.let{ SubjectActivity.startActivity(context, it) }
        }

        subjectView.topicAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.topicAdapter.data[position].url, "")
        }

        subjectView.blogAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.blogAdapter.data[position].url)
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
    private fun openEpisode(episode: Episode, subject: Subject){
        val view = context.layoutInflater.inflate(R.layout.dialog_epsode, context.item_collect, false)
        view.item_episode_title.text = if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else "首播：" + episode.airdate + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else "时长：" + episode.duration + "\n") +
                "讨论 (+" + episode.comment + ")"
        view.item_episode_title.setOnClickListener {
            WebActivity.launchUrl(context, "${Bangumi.SERVER}/m/topic/ep/${episode.id}", "")
        }
        view.item_episode_status.setSelection(intArrayOf(3,1,0,2)[episode.progress?.status?.id?:0])
        userModel.getToken()?.let { token ->
            view.item_episode_status.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    val newStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.types[position]
                    api.updateProgress(episode.id, newStatus, token.access_token ?: "").enqueue(
                            ApiHelper.buildCallback(context, {
                                refreshProgress(subject)
                            }, {}))
                } }
        }?:{
            view.item_episode_status.visibility = View.GONE
        }()
        AlertDialog.Builder(context)
                .setView(view).show()
    }

    private var subjectCall : Call<Subject>? = null
    private fun refreshSubject(subject: Subject){
        //context.data_layout.visibility = View.GONE
        //context.subject_swipe.isRefreshing = true
        subjectCall?.cancel()
        subjectCall = api.subject(subject.id)
        subjectCall?.enqueue(ApiHelper.buildCallback(context, {
            refreshLines(it)
            subjectView.updateSubject(it)
        }, {}))

        BgmIpViewer.createInstance().subject(subject.id).enqueue(ApiHelper.buildCallback(context, {
            val bgmIp = it.nodes?.firstOrNull { it.subject_id == subject.id }?:return@buildCallback
            val id = it.edges?.firstOrNull{edge-> edge.source == bgmIp.id && edge.relation == "主线故事"}?.target?:bgmIp.id
            val ret = ArrayList<IpView.Node>()
            it.edges?.filter { edge-> edge.target == id && edge.relation == "主线故事" }?.reversed()?.forEach { edge->
                ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
            }
            ret.add(0,it.nodes.firstOrNull { it.id == id }?:return@buildCallback)
            var prevId = id
            while(true){
                prevId = it.edges?.firstOrNull { it.source == prevId && it.relation == "前传"}?.target?:break
                it.edges.filter { edge-> edge.target == prevId && edge.relation == "主线故事" }.reversed().forEach {edge->
                    ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
                ret.add(0, it.nodes.firstOrNull{it.id == prevId}?:break)
            }
            var nextId = id
            while(true){
                nextId = it.edges?.firstOrNull { it.source == nextId && it.relation == "续集"}?.target?:break
                ret.add(it.nodes.firstOrNull{it.id == nextId}?:break)
                it.edges.filter { edge-> edge.target == nextId && edge.relation == "主线故事" }.forEach {edge->
                    ret.add(it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
            }
            if(ret.size > 1){
                subjectView.seasonAdapter.setNewData(ret.distinct())
                subjectView.seasonAdapter.currentId = bgmIp.id
                subjectView.seasonLayoutManager.scrollToPositionWithOffset(subjectView.seasonAdapter.data.indexOfFirst { it.id == bgmIp.id }, 0)
            }
        }, {}))

        Bangumi.getSubject(subject).enqueue(ApiHelper.buildCallback(context, {
            subjectView.linkedSubjectsAdapter.setNewData(it)
            //Log.v("list", it.toString())
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
        if(page == 1)
            subjectView.commentAdapter.setNewData(null)
        Bangumi.getComments(subject, page).enqueue(ApiHelper.buildCallback(context, {
            if(it?.isEmpty() == true)
                subjectView.commentAdapter.loadMoreEnd()
            else{
                subjectView.commentAdapter.loadMoreComplete()
                subjectView.commentAdapter.addData(it)
            }
        }, {subjectView.commentAdapter.loadMoreFail()}))
    }

    private fun refreshLines(subject: Subject){
        val dateList = subject.air_date?.split("-") ?: return
        val year = dateList.getOrNull(0)?.toIntOrNull()?:0
        val month = dateList.getOrNull(1)?.toIntOrNull()?:1
        BangumiData.createInstance().query(year, String.format("%02d", month)).enqueue(ApiHelper.buildCallback(context, {
            subjectView.sitesAdapter.setNewData(null)
            it.filter { it.sites?.filter { it.site == "bangumi" }?.getOrNull(0)?.id?.toIntOrNull() == subject.id }.forEach {
                if(subjectView.sitesAdapter.data.size == 0 && !it.officialSite.isNullOrEmpty())
                    subjectView.sitesAdapter.addData(BangumiItem.SitesBean("official", "", it.officialSite))
                subjectView.sitesAdapter.addData(it.sites?.filter { it.site != "bangumi" }?:ArrayList())
            }
        }, {}))
    }

    private fun refreshCollection(subject: Subject){
        userModel.getToken()?.let{token ->
            //Log.v("token", token.toString())
            api.collectionStatus(subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, { body->
                val status = body.status
                if(status != null){
                    context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                            if(status.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
                    context.item_collect_info.text = status.name?:""
                }

                context.item_collect.setOnClickListener {
                    val view = context.layoutInflater.inflate(R.layout.dialog_edit_subject, context.item_collect, false)
                    if(status != null){
                        view.item_status.setSelection(status.id-1)
                        view.item_rating.rating = body.rating.toFloat()
                        view.item_comment.setText(body.comment)
                        view.item_private.isChecked = body.private == 1
                    }
                    AlertDialog.Builder(context)
                            .setView(view)
                            .setPositiveButton("提交"){ _: DialogInterface, _: Int ->
                                val newStatus = CollectionStatusType.status[view.item_status.selectedItemId.toInt()]
                                val newRating = view.item_rating.rating.toInt()
                                val newComment = view.item_comment.text.toString()
                                val newPrivacy = if(view.item_private.isChecked) 1 else 0
                                //Log.v("new", "$new_status,$new_rating,$new_comment")
                                api.updateCollectionStatus(subject.id, token.access_token?:"",
                                        newStatus, newComment, newRating, newPrivacy).enqueue(ApiHelper.buildCallback(context,{},{
                                    refreshCollection(subject)
                                }))
                            }.show()
                }
            }, {}))
        }
    }

    private fun refreshProgress(subject: Subject){
        userModel.getToken()?.let{token ->
            api.progress(token.user_id.toString(), subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, {
                subjectView.progress = it
            }, {
                subjectView.progress = null
                subjectView.loadedProgress = true }))
        }
    }
}