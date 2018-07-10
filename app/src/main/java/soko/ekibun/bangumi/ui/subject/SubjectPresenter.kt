package soko.ekibun.bangumi.ui.subject

import android.content.DialogInterface
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import kotlinx.android.synthetic.main.subject_blog.*
import kotlinx.android.synthetic.main.subject_topic.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.api.bangumiData.BangumiData
import soko.ekibun.bangumi.api.bangumiData.bean.BangumiItem
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.web.WebActivity

class SubjectPresenter(private val context: SubjectActivity){
    val api by lazy { Bangumi.createInstance() }
    val subjectView by lazy{ SubjectView(context) }
    private val userModel by lazy { UserModel(context) }

    fun init(subject: Subject){
        subjectView.updateSubject(subject)
        refreshSubject(subject)
        refreshProgress(subject)
        refreshCollection(subject)

        context.item_detail.setOnClickListener {
            WebActivity.launchUrl(context, subject.url)
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
            WebActivity.launchUrl(context, subjectView.topicAdapter.data[position].url)
        }

        subjectView.blogAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, subjectView.blogAdapter.data[position].url)
        }

        context.topic_detail.setOnClickListener{
            WebActivity.launchUrl(context, "${subject.url}/board")
        }

        context.blog_detail.setOnClickListener{
            WebActivity.launchUrl(context, "${subject.url}/reviews")
        }

        subjectView.sitesAdapter.setOnItemClickListener { _, _, position ->
            try{
                CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(subjectView.sitesAdapter.data[position].parseUrl()))
            }catch (e: Exception){ e.printStackTrace() }
        }
    }

    private fun openEpisode(episode: Episode, subject: Subject){
        userModel.getToken()?.let { token ->
            val status = context.resources.getStringArray(R.array.episode_status)
            val dialog = AlertDialog.Builder(context)
                    .setItems(status) { _, which ->
                        api.updateProgress(episode.id, SubjectProgress.EpisodeProgress.EpisodeStatus.types[which],token.access_token?:"").enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                    }.create()
            //dialog.window.setGravity(Gravity.BOTTOM)
            dialog.show()
        }
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

        Bangumi.getSubject(subject).enqueue(ApiHelper.buildCallback(context, {
            subjectView.linkedSubjectsAdapter.setNewData(it)
            //Log.v("list", it.toString())
        }, {}))
    }

    private fun refreshLines(subject: Subject){
        val dateList = subject.air_date?.split("-") ?: return
        val year = dateList.getOrNull(0)?.toIntOrNull()?:0
        val month = dateList.getOrNull(1)?.toIntOrNull()?:1
        BangumiData.createInstance().query(year, String.format("%02d", month)).enqueue(ApiHelper.buildCallback(context, {
            subjectView.sitesAdapter.setNewData(null)
            it.filter { it.sites?.filter { it.site == "bangumi" }?.getOrNull(0)?.id?.toIntOrNull() == subject.id }.forEach {
                if(subjectView.sitesAdapter.data.size == 0)
                    subjectView.sitesAdapter.addData(BangumiItem.SitesBean("offical", it.officialSite))
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
                subjectView.loaded_progress = true }))
        }
    }
}