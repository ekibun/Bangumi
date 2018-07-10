package soko.ekibun.bangumi.ui.video

import android.content.DialogInterface
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_edit_lines.view.*
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import kotlinx.android.synthetic.main.video_buttons.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.model.ParseInfoModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.api.parser.ParseInfo
import soko.ekibun.bangumi.util.JsonUtil
import android.widget.ListPopupWindow
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_blog.*
import kotlinx.android.synthetic.main.subject_topic.*
import soko.ekibun.bangumi.api.bangumiData.BangumiData
import soko.ekibun.bangumi.api.bangumiData.bean.BangumiItem
import soko.ekibun.bangumi.model.ParseModel
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.web.WebActivity

class SubjectPresenter(private val context: VideoActivity){
    val api by lazy { Bangumi.createInstance() }
    val subjectView by lazy{ SubjectView(context) }
    private val userModel by lazy { UserModel(context) }
    private val parseInfoModel by lazy { ParseInfoModel(context) }

    val subject by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_SUBJECT), Subject::class.java) }

    init{
        subjectView.updateSubject(subject)
        refreshSubject(subject)
        refreshCollection(subject)
        refreshProgress(subject)

        context.item_detail.setOnClickListener {
            WebActivity.launchUrl(context, subject.url)
        }

        context.videoPresenter.doPlay = { position: Int ->
            val episode = subjectView.episodeDetailAdapter.data[position]?.t
            if(episode != null){
                parseInfoModel.getInfo(subject)?.let{
                    if(it.video?.id.isNullOrEmpty()) {
                        episode.url?.let{ WebActivity.launchUrl(context, it) }
                        return@let
                    }
                    val episodePrev = subjectView.episodeDetailAdapter.data.getOrNull(position-1)?.t
                    val episodeNext = subjectView.episodeDetailAdapter.data.getOrNull(position+1)?.t
                    context.videoPresenter.prev = if(episodePrev == null || (episodePrev.status?:"") !in listOf("Air")) null else position - 1
                    context.videoPresenter.next = if(episodeNext == null || (episodeNext.status?:"") !in listOf("Air")) null else position + 1
                    context.runOnUiThread { context.videoPresenter.play(episode, it) }
                }?:episode.url?.let{ WebActivity.launchUrl(context, it) }
            }
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

        subjectView.linkedSubjectsAdapter.setOnItemClickListener { _, _, position ->
            subjectView.linkedSubjectsAdapter.data[position]?.let{
                SubjectActivity.startActivity(context, it)
            }
        }

        subjectView.episodeAdapter.setOnItemClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{episode->
                context.videoPresenter.doPlay(subjectView.episodeDetailAdapter.data.indexOfFirst { it.t == episode })
            }
        }

        subjectView.episodeAdapter.setOnItemLongClickListener { _, _, position ->
            userModel.getToken()?.let{token->
                subjectView.episodeAdapter.data[position]?.let{ep->
                    val status = context.resources.getStringArray(R.array.episode_status)
                    val dialog = AlertDialog.Builder(context)
                            .setItems(status) { _, which ->
                                api.updateProgress(ep.id, SubjectProgress.EpisodeProgress.EpisodeStatus.types[which],token.access_token?:"").enqueue(
                                        ApiHelper.buildCallback(context, {
                                            refreshProgress(subject)
                                        }, {}))
                            }.create()
                    //dialog.window.setGravity(Gravity.BOTTOM)
                    dialog.show()
                }
            }
            true
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.videoPresenter.doPlay(position)
        }

        subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            userModel.getToken()?.let{token->
                subjectView.episodeDetailAdapter.data[position]?.t?.let{ep->
                    val status = context.resources.getStringArray(R.array.episode_status)
                    val dialog = AlertDialog.Builder(context)
                            .setItems(status) { _, which ->
                                api.updateProgress(ep.id, SubjectProgress.EpisodeProgress.EpisodeStatus.types[which],token.access_token?:"").enqueue(
                                        ApiHelper.buildCallback(context, {
                                            refreshProgress(subject)
                                        }, {}))
                            }.create()
                    //dialog.window.setGravity(Gravity.BOTTOM)
                    dialog.show()
                }
            }
            true
        }

        /*context.nested_scroll.setOnScrollChangeListener { v: NestedScrollView, _: Int, _: Int, oldScrollX: Int, oldScrollY: Int ->
            if(v.tag == true){
                v.tag = null
                v.scrollTo(oldScrollX, oldScrollY)
                v.smoothScrollTo(oldScrollX, oldScrollY) } }*/
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
            val list = ArrayList<BangumiItem.SitesBean>()
            it.filter { it.sites?.filter { it.site == "bangumi" }?.getOrNull(0)?.id?.toIntOrNull() == subject.id }.forEach {
                if(list.size == 0)
                    list.add(BangumiItem.SitesBean("offical", it.officialSite))
                list.addAll(it.sites?.filter { it.site != "bangumi" }?:ArrayList())
            }
            context.cl_lines.setOnClickListener {
                val popList = ListPopupWindow(context)
                popList.anchorView = context.cl_lines
                popList.setAdapter(SitesAdapter(context, list))
                popList.isModal = true
                popList.show()

                popList.listView.setOnItemClickListener { _, _, position, _ ->
                    try{
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(list[position].parseUrl()))
                    }catch (e: Exception){ e.printStackTrace() }
                    popList.dismiss()
                }

                popList.listView.setOnItemLongClickListener { _, _, position, _ ->
                    ParseModel.processUrl(list[position].parseUrl()){context.runOnUiThread {
                        val view = context.layoutInflater.inflate(R.layout.dialog_edit_lines, context.cl_lines, false)
                        view.item_video_type.setSelection(it.type)
                        view.item_video_id.setText(it.id)
                        view.item_danmaku_type.setSelection(it.type)
                        view.item_danmaku_id.setText(it.id)
                        AlertDialog.Builder(context)
                                .setView(view)
                                .setPositiveButton("提交"){ _: DialogInterface, _: Int ->
                                    val parseInfo = ParseInfo(view.item_api.text.toString(),
                                            ParseInfo.ParseItem(view.item_video_type.selectedItemId.toInt(), view.item_video_id.text.toString()),
                                            ParseInfo.ParseItem(view.item_danmaku_type.selectedItemId.toInt(), view.item_danmaku_id.text.toString()))
                                    parseInfoModel.saveInfo(subject, parseInfo)
                                    refreshLines(subject)
                                }.show()
                    } }
                    popList.dismiss()
                    true
                }
            }
        }, {}))
        val info = parseInfoModel.getInfo(subject)
        context.tv_lines.text = info?.video?.let{ context.resources.getStringArray(R.array.parse_type)[it.type]}?:context.resources.getString(R.string.lines)

        context.cl_lines.setOnLongClickListener {
            val view = context.layoutInflater.inflate(R.layout.dialog_edit_lines, context.cl_lines, false)
            info?.let{
                view.item_api.setText(it.api)
                view.item_video_type.setSelection(it.video?.type?:0)
                view.item_video_id.setText(it.video?.id)
                view.item_danmaku_type.setSelection(it.danmaku?.type?:0)
                view.item_danmaku_id.setText(it.danmaku?.id)
            }
            AlertDialog.Builder(context)
                    .setView(view)
                    .setPositiveButton("提交"){ _: DialogInterface, _: Int ->
                        val parseInfo = ParseInfo(view.item_api.text.toString(),
                                ParseInfo.ParseItem(view.item_video_type.selectedItemId.toInt(), view.item_video_id.text.toString()),
                                ParseInfo.ParseItem(view.item_danmaku_type.selectedItemId.toInt(), view.item_danmaku_id.text.toString()))
                        parseInfoModel.saveInfo(subject, parseInfo)
                        refreshLines(subject)
                    }.show()
            true
        }
    }

    private fun refreshCollection(subject: Subject){
        userModel.getToken()?.let{token ->
            //Log.v("token", token.toString())
            api.collectionStatus(subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, { body->
                val status = body.status
                if(status != null){
                    context.iv_chase.setImageDrawable(context.resources.getDrawable(
                            if(status.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
                    context.tv_chase.text = status.name?:""
                }

                context.cl_chase.setOnClickListener {
                    val view = context.layoutInflater.inflate(R.layout.dialog_edit_subject, context.cl_chase, false)
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
}