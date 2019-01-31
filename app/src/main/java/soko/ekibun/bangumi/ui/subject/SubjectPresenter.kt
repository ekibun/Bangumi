package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AlertDialog
import android.view.*
import android.widget.PopupMenu
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.view.*
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import kotlinx.android.synthetic.main.subject_blog.view.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_character.view.*
import kotlinx.android.synthetic.main.subject_topic.view.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.github.GithubRaw
import soko.ekibun.bangumi.api.github.bean.BangumiItem
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

    fun refresh(subject: Subject){
        refreshSubject(subject)
        refreshProgress(subject)
        refreshCollection(subject)
    }

    @SuppressLint("SetTextI18n")
    fun init(subject: Subject){
        subjectView.updateSubject(subject)
        refresh(subject)

        subjectView.detail.character_detail.setOnClickListener {
            WebActivity.launchUrl(context, "${subject.url}/characters")
        }

        context.title_expand.setOnClickListener {
            WebActivity.launchUrl(context, subject.url)
        }
        context.title_collapse.setOnClickListener {
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
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let{ openEpisode(it, subject, eps) }
            true
        }
        subjectView.episodeAdapter.setOnItemClickListener { _, view, position ->
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let { userModel.getToken()?.let { token ->
                val epStatus = context.resources.getStringArray(R.array.episode_status)

                val popupMenu = PopupMenu(context, view)
                epStatus.forEachIndexed { index, s ->
                    popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
                popupMenu.setOnMenuItemClickListener {menu->
                    val index = menu.itemId - Menu.FIRST

                    if (index == 1) {//看到
                        val epIds = eps.map { it.id.toString() }.reduce { acc, s -> "$acc,$s" }
                        api.updateProgress(eps.last().id, SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, token.access_token
                                ?: "", epIds).enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                    } else {
                        val newStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.types[if (index > 1) index - 1 else index]
                        api.updateProgress(it.id, newStatus, token.access_token ?: "").enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                    }
                    false
                }
                popupMenu.show()
            } ?: { openEpisode(it, subject, eps) }() }
            //subjectView.episodeAdapter.data[position]?.let{ WebActivity.launchUrl(context, it.url) }
        }

        userModel.getToken()?.let { token ->

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
                    val epIds = eps.map{ it.t.id.toString()}.reduce { acc, s -> "$acc,$s" }
                    if(newStatus == SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH)
                        api.updateProgress(eps.last().t.id, newStatus, token.access_token ?: "", epIds).enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                    else for(ep in eps) //issues 25
                        api.updateProgress(ep.t.id, newStatus, token.access_token ?: "").enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                    false
                }
                popupMenu.show()
            }
            subjectView.episodeDetailAdapter.updateSelection = {
                val eps = subjectView.episodeDetailAdapter.data.filter { it.isSelected }
                context.item_edit_ep.visibility = if(eps.isEmpty()) View.GONE else View.VISIBLE
                context.item_ep_title.text = "${context.getText(R.string.episodes)}${if(eps.isEmpty()) "" else "(${eps.size})"}"
            }

            subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
                subjectView.episodeDetailAdapter.longClickListener(position)
            }
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            val eps = subjectView.episodeDetailAdapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            if(subjectView.episodeDetailAdapter.clickListener(position))
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
        val view = context.layoutInflater.inflate(R.layout.dialog_epsode, context.item_collect, false)
        view.item_episode_title.text = episode.parseSort() + " " + if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else "首播：" + episode.airdate + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else "时长：" + episode.duration + "\n") +
                "讨论 (+" + episode.comment + ")"
        view.item_episode_title.setOnClickListener {
            WebActivity.launchUrl(context, "${Bangumi.SERVER}/m/topic/ep/${episode.id}", "")
        }
        when(episode.progress?.status?.id?:0){
            1 -> view.radio_queue.isChecked = true
            2 -> view.radio_watch.isChecked = true
            3 -> view.radio_drop.isChecked = true
            else -> view.radio_remove.isChecked = true
        }
        //view.item_episode_status.setSelection(intArrayOf(4,2,0,3)[episode.progress?.status?.id?:0])
        userModel.getToken()?.let { token ->
            view.item_episode_status.setOnCheckedChangeListener { _, checkedId ->
                val newStatus = when(checkedId){
                    R.id.radio_watch_to ->{
                        val epIds = eps.map{ it.id.toString()}.reduce { acc, s -> "$acc,$s" }
                        api.updateProgress(eps.last().id, SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, token.access_token ?: "", epIds).enqueue(
                                ApiHelper.buildCallback(context, {
                                    refreshProgress(subject)
                                }, {}))
                        return@setOnCheckedChangeListener }
                    R.id.radio_watch -> SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH
                    R.id.radio_queue -> SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE
                    R.id.radio_drop -> SubjectProgress.EpisodeProgress.EpisodeStatus.DROP
                    else -> SubjectProgress.EpisodeProgress.EpisodeStatus.REMOVE }
                api.updateProgress(episode.id, newStatus, token.access_token ?: "").enqueue(
                        ApiHelper.buildCallback(context, {
                            refreshProgress(subject)
                        }, {}))
            }
        }?:{
            view.item_episode_status.visibility = View.GONE
        }()
        showDialog(view)
    }

    private fun showDialog(view: View): Dialog{
        val dialog = Dialog(context, R.style.AppTheme_Dialog_Floating)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.attributes?.let{
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.attributes = it
        }
        dialog.window?.setWindowAnimations(R.style.AnimDialog)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        return dialog
    }

    private var subjectCall : Call<Subject>? = null
    private fun refreshSubject(subject: Subject){
        subjectCall?.cancel()
        subjectCall = Bangumi.getSubject(subject)
        subjectCall?.enqueue(ApiHelper.buildCallback(context, {
            refreshLines(it)
            subjectView.updateSubject(it)
        }, {}))

        api.subjectEp(subject.id).enqueue(ApiHelper.buildCallback(context, {
            subjectView.updateEpisode(it)
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
            subjectView.detail.item_comment_header.visibility = View.VISIBLE
        }, {subjectView.commentAdapter.loadMoreFail()}))
    }

    private fun refreshLines(subject: Subject){
        val dateList = subject.air_date?.split("-") ?: return
        val year = dateList.getOrNull(0)?.toIntOrNull()?:0
        val month = dateList.getOrNull(1)?.toIntOrNull()?:1
        GithubRaw.createInstance().bangumiData(year, String.format("%02d", month)).enqueue(ApiHelper.buildCallback(context, {
            subjectView.sitesAdapter.setNewData(null)
            it.filter { it.sites?.filter { it.site == "bangumi" }?.getOrNull(0)?.id?.toIntOrNull() == subject.id }.forEach {
                if(subjectView.sitesAdapter.data.size == 0 && !it.officialSite.isNullOrEmpty())
                    subjectView.sitesAdapter.addData(BangumiItem.SitesBean("official", "", it.officialSite))
                subjectView.sitesAdapter.addData(it.sites?.filter { it.site != "bangumi" }?:ArrayList())
            }
            subjectView.detail.site_list.visibility = if(subjectView.sitesAdapter.data.isEmpty()) View.GONE else View.VISIBLE
        }, {}))
    }

    private fun removeCollection(subject: Subject){
        AlertDialog.Builder(context).setTitle("删除这个条目收藏？")
                .setNegativeButton("取消") { _, _ -> }.setPositiveButton("确定") { _, _ ->
                    ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/${subject.id}/remove?gh=${context.formhash}", mapOf("User-Agent" to context.ua)){ it.code() == 200 }
                            .enqueue(ApiHelper.buildCallback(context, {
                                refreshCollection(subject)
                            }, {}))
                }.show()
    }

    private fun refreshCollection(subject: Subject){
        userModel.getToken()?.let{token ->
            //Log.v("token", token.toString())
            api.collectionStatus(subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, { body->
                val status = body.status
                context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                        if(status?.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
                context.item_collect_info.text = status?.name?:context.getString(R.string.collect)

                context.item_collect.setOnClickListener{
                    val popupMenu = PopupMenu(context, context.item_collect)
                    val statusList = context.resources.getStringArray(R.array.collection_status)
                    statusList.forEachIndexed { index, s ->
                        popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
                    if(status != null && context.formhash.isNotEmpty())
                        popupMenu.menu.add(Menu.NONE, Menu.FIRST + statusList.size, statusList.size, "删除")
                    popupMenu.setOnMenuItemClickListener {menu->
                        if(menu.itemId == Menu.FIRST + statusList.size){
                            removeCollection(subject)
                            return@setOnMenuItemClickListener false
                        }
                        val newStatus = CollectionStatusType.status[menu.itemId - Menu.FIRST]
                        val newTags = if(body.tag?.isNotEmpty() == true) body.tag.reduce { acc, s -> "$acc $s" } else ""
                        api.updateCollectionStatus(subject.id, token.access_token?:"",
                                newStatus, newTags, body.comment, body.rating, body.private).enqueue(ApiHelper.buildCallback(context,{},{
                            refreshCollection(subject)
                        }))
                        false
                    }
                    popupMenu.show()
                }

                context.item_collect.setOnLongClickListener {
                    EditSubjectDialog.showDialog(context, subject, body, context.formhash, token.access_token?:""){
                        if(it) removeCollection(subject)
                        else refreshCollection(subject)
                    }
                    /*
                    val view = context.layoutInflater.inflate(R.layout.dialog_edit_subject, context.item_collect, false)
                    view.item_subject_title.text = subject.getPrettyName()
                    val selectMap = mapOf(
                            CollectionStatusType.WISH to R.id.radio_wish,
                            CollectionStatusType.COLLECT to R.id.radio_collect,
                            CollectionStatusType.DO to R.id.radio_do,
                            CollectionStatusType.ON_HOLD to R.id.radio_hold,
                            CollectionStatusType.DROPPED to R.id.radio_dropped)
                    val adapter = EditTagAdapter()
                    val layoutManager = LinearLayoutManager(context)
                    layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                    view.item_tag_list.layoutManager = layoutManager
                    view.item_tag_list.adapter = adapter
                    if(status != null){
                        view.item_subject_status.check(selectMap[status.type]!!)
                        view.item_rating.rating = body.rating.toFloat()
                        view.item_comment.setText(body.comment)
                        view.item_private.isChecked = body.private == 1
                        adapter.setNewData(body.tag?.filter { it.isNotEmpty() })
                    }
                    val dialog = showDialog(view)
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    view.item_remove.visibility = if(context.formhash.isEmpty()) View.INVISIBLE else View.VISIBLE
                    view.item_tag_add.setOnClickListener {
                        val editText = EditText(context)
                        AlertDialog.Builder(context)
                                .setView(editText)
                                .setTitle("添加标签")
                                .setPositiveButton("提交"){ _, _ ->
                                    adapter.addData(editText.text.split(" ").filter { it.isNotEmpty() })
                                }.show()
                    }

                    view.item_remove.setOnClickListener {
                        dialog.dismiss()
                        removeCollection(subject)
                    }
                    view.item_outside.setOnClickListener {
                        dialog.dismiss()
                    }
                    view.item_submit.setOnClickListener {
                        dialog.dismiss()
                        val newStatus = selectMap.toList().first { it.second == view.item_subject_status.checkedRadioButtonId }.first
                        val newRating = view.item_rating.rating.toInt()
                        val newComment = view.item_comment.text.toString()
                        val newPrivacy = if(view.item_private.isChecked) 1 else 0
                        val newTags = if(adapter.data.isNotEmpty()) adapter.data.reduce { acc, s -> "$acc $s" } else ""
                        api.updateCollectionStatus(subject.id, token.access_token?:"",
                                newStatus, newTags, newComment, newRating, newPrivacy).enqueue(ApiHelper.buildCallback(context,{},{
                            refreshCollection(subject)
                        }))
                    }*/
                    true
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