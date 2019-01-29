package soko.ekibun.bangumi.ui.subject

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.xiaofeng.flowlayoutmanager.FlowLayoutManager
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.view.*
import kotlinx.android.synthetic.main.subject_blog.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_character.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import kotlinx.android.synthetic.main.subject_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.PlayerBridge

class SubjectView(private val context: SubjectActivity){
    val episodeAdapter = SmallEpisodeAdapter()
    val episodeDetailAdapter = EpisodeAdapter()
    val linkedSubjectsAdapter = LinkedSubjectAdapter()
    val commendSubjectsAdapter = LinkedSubjectAdapter()
    val characterAdapter = CharacterAdapter()
    val tagAdapter = TagAdapter()
    val topicAdapter = TopicAdapter()
    val blogAdapter = BlogAdapter()
    val sitesAdapter = SitesAdapter()
    val commentAdapter = CommentAdapter()
    val seasonAdapter = SeasonAdapter()
    val seasonLayoutManager = LinearLayoutManager(context)

    val detail: LinearLayout = context.subject_detail

    init{
        val marginEnd = (context.item_buttons.layoutParams as CollapsingToolbarLayout.LayoutParams).marginEnd
        (context.title_expand.layoutParams as ConstraintLayout.LayoutParams).marginEnd = 3 * marginEnd

        var nestScrollDistance = context.app_bar.totalScrollRange
        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ appBarLayout, verticalOffset ->
             val ratio = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            nestScrollDistance = appBarLayout.totalScrollRange + verticalOffset
            context.item_scrim.alpha = ratio
            context.item_subject.alpha = 1 - ratio
            context.item_buttons.translationY = -(context.toolbar.height - context.item_buttons.height * 9 / 8) * ratio / 2 - context.item_buttons.height / 16
            context.title_collapse.alpha = 1-(1-ratio)*(1-ratio)*(1-ratio)
            context.title_expand.alpha = 1-ratio

            context.item_buttons.translationX = -2.2f * marginEnd * ratio
        })

        context.season_list.adapter = seasonAdapter
        seasonLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.season_list.layoutManager = seasonLayoutManager
        context.season_list.isNestedScrollingEnabled = false

        context.episode_list.adapter = episodeAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.episode_list.layoutManager = layoutManager
        context.episode_list.isNestedScrollingEnabled = false
        val swipeTouchListener = View.OnTouchListener{ v, _ ->
            if((v as? RecyclerView)?.canScrollHorizontally(1) == true || (v as? RecyclerView)?.canScrollHorizontally(-1) == true)
                context.shouldCancelActivity = false
            false
        }
        context.episode_list.setOnTouchListener(swipeTouchListener)
        context.season_list.setOnTouchListener(swipeTouchListener)
        context.commend_list.setOnTouchListener(swipeTouchListener)
        context.linked_list.setOnTouchListener(swipeTouchListener)
        context.character_list.setOnTouchListener(swipeTouchListener)
        context.tag_list.setOnTouchListener(swipeTouchListener)

        val touchListener = episodeDetailAdapter.setUpWithRecyclerView(context.episode_detail_list)
        touchListener.nestScrollDistance = {
            nestScrollDistance
        }
        context.episode_detail_list.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        context.item_close.setOnClickListener {
            closeEpisodeDetail()
        }
        context.episode_detail.setOnClickListener{
            showEpisodeDetail(true)
        }

        context.linked_list.adapter = linkedSubjectsAdapter
        val subjectLayoutManager = LinearLayoutManager(context)
        subjectLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.linked_list.layoutManager = subjectLayoutManager
        context.linked_list.isNestedScrollingEnabled = false


        context.commend_list.adapter = commendSubjectsAdapter
        val subjectLayoutManager2 = LinearLayoutManager(context)
        subjectLayoutManager2.orientation = LinearLayoutManager.HORIZONTAL
        context.commend_list.layoutManager = subjectLayoutManager2
        context.commend_list.isNestedScrollingEnabled = false

        context.character_list.adapter = characterAdapter
        val characterLayoutManager = LinearLayoutManager(context)
        characterLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.character_list.layoutManager = characterLayoutManager
        context.character_list.isNestedScrollingEnabled = false

        context.topic_list.adapter = topicAdapter
        context.topic_list.layoutManager = LinearLayoutManager(context)
        context.topic_list.isNestedScrollingEnabled = false

        context.blog_list.adapter = blogAdapter
        context.blog_list.layoutManager = LinearLayoutManager(context)
        context.blog_list.isNestedScrollingEnabled = false

        context.site_list.adapter = sitesAdapter
        val flowLayoutManager = FlowLayoutManager()
        flowLayoutManager.isAutoMeasureEnabled = true
        context.site_list.layoutManager = flowLayoutManager
        context.site_list.isNestedScrollingEnabled = false

        context.tag_list.adapter = tagAdapter
        val tagLayoutManager = LinearLayoutManager(context)
        tagLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.tag_list.layoutManager = tagLayoutManager
        context.tag_list.isNestedScrollingEnabled = false

        context.comment_list.adapter = commentAdapter
        context.comment_list.layoutManager = LinearLayoutManager(context)

        detail.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        context.root_layout.removeView(detail)
        commentAdapter.setHeaderView(detail)
    }

    fun closeEpisodeDetail(){
        val eps = episodeDetailAdapter.data.filter { it.isSelected }
        if(eps.isEmpty())
            showEpisodeDetail(false)
        else{
            for(ep in eps) ep.isSelected = false
            episodeDetailAdapter.updateSelection()
            episodeDetailAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: Subject){
        if(context.isDestroyed) return

        context.title_collapse.text = subject.getPrettyName()
        context.title_expand.text = context.title_collapse.text
        context.title_expand.post {
            val layoutParams = (context.title_collapse.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.marginEnd = 3 * (context.item_buttons.layoutParams as CollapsingToolbarLayout.LayoutParams).marginEnd + context.item_buttons.width
            context.title_collapse.layoutParams = layoutParams
            (context.item_subject.layoutParams as CollapsingToolbarLayout.LayoutParams).topMargin = context.toolbar_container.height
        }
        context.item_info.text = if(subject.typeString.isNullOrEmpty()) SubjectType.getDescription(subject.type) else subject.typeString
        context.item_subject_title.visibility = View.GONE
        val saleDate = subject.infobox?.firstOrNull { it.first in arrayOf("发售日期", "发售日", "发行日期") }
        val artist = subject.infobox?.firstOrNull { it.first.substringBefore(" ") in arrayOf("动画制作", "作者", "开发", "游戏制作", "艺术家") }
                ?:subject.infobox?.firstOrNull { it.first.substringBefore(" ") in arrayOf("导演", "发行") }
        context.item_air_time.text = if(saleDate!= null) "${saleDate.first}：${saleDate.second}"
            else "放送日期：${subject.air_date?:""} ${if(artist != null)CalendarAdapter.weekSmall[subject.air_weekday] else ""}"
        context.item_air_week.text = if(artist != null) "${artist.first}：${artist.second}" else "更新时间：${CalendarAdapter.weekSmall[subject.air_weekday]}"
        detail.item_detail.text = subject.summary

        context.item_play.visibility = if(PlayerBridge.checkActivity(context) && subject.type in listOf(SubjectType.ANIME, SubjectType.REAL)) View.VISIBLE else View.GONE

        subject.rating?.let {
            context.item_score.text = it.score.toString()
            context.item_score_count.text = context.getString(R.string.rate_count, it.total)
        }
        Glide.with(context.item_cover)
                .load(subject.images?.getImage(context))
                .apply(RequestOptions.placeholderOf(context.item_cover.drawable))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(context.item_cover)
        context.item_cover.setOnClickListener {
            val popWindow = PopupWindow(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
            val photoView = DragPhotoView(it.context)
            popWindow.contentView = photoView
            Glide.with(photoView).load(subject.images?.large)
                    .apply(RequestOptions.placeholderOf(context.item_cover.drawable))
                    .into(photoView)
            photoView.mTapListener={
                popWindow.dismiss()
            }
            photoView.mExitListener={
                popWindow.dismiss()
            }
            photoView.mLongClickListener = {
                val systemUiVisibility = popWindow.contentView.systemUiVisibility
                AlertDialog.Builder(context)
                        .setItems(arrayOf("分享"))
                        { _, _ ->
                            AppUtil.shareDrawable(context, photoView.drawable)
                        }.setOnDismissListener {
                            popWindow.contentView.systemUiVisibility = systemUiVisibility
                        }.show()
            }
            popWindow.isClippingEnabled = false
            popWindow.animationStyle = R.style.AppTheme_FadeInOut
            popWindow.showAtLocation(it, Gravity.CENTER, 0, 0)
            popWindow.contentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        Glide.with(context.item_cover_blur)
                .load(subject.images?.getImage(context))
                .apply(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(context.item_cover_blur)
        ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)!!})?.let{
            updateEpisode(it)
        }
        detail.item_topics.visibility = if(subject.topic?.isNotEmpty() == true) View.VISIBLE else View.GONE
        topicAdapter.setNewData(subject.topic)
        detail.item_blogs.visibility = if(subject.blog?.isNotEmpty() == true) View.VISIBLE else View.GONE
        blogAdapter.setNewData(subject.blog)
        detail.item_character.visibility = if(subject.crt?.isNotEmpty() == true) View.VISIBLE else View.GONE
        characterAdapter.setNewData(subject.crt)
        detail.item_linked.visibility = if(subject.linked?.isNotEmpty() == true) View.VISIBLE else View.GONE
        linkedSubjectsAdapter.setNewData(subject.linked)
        detail.item_commend.visibility = if(subject.commend?.isNotEmpty() == true) View.VISIBLE else View.GONE
        commendSubjectsAdapter.setNewData(subject.commend)

        tagAdapter.setNewData(subject.tags)
        tagAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, "${Bangumi.SERVER}/${SubjectType.typeNameMap[subject.type]}/tag/${tagAdapter.data[position].first}")
        }
    }

    fun updateEpisode(subject: Subject){
        ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)!!})?.let{
            updateEpisode(it)
        }
    }

    private fun updateEpisode(episodes: List<Episode>){
        if(episodes.none { it.id != 0 }) return
        val mainEps = episodes.filter { it.type == Episode.TYPE_MAIN }
        val eps = mainEps.filter { (it.status ?: "") in listOf("Air") }.size
        context.episode_detail?.text = context.getString(if(eps == mainEps.size) R.string.phrase_full else R.string.phrase_updating, eps)

        val maps = LinkedHashMap<String, List<Episode>>()
        episodes.forEach {
            val key = it.cat?:Episode.getTypeName(it.type)
            maps[key] = (maps[key]?:ArrayList()).plus(it)
        }
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
            episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(true, it.key))
            it.value.forEach { ep ->
                if((ep.status?:"") in listOf("Air"))
                    episodeAdapter.addData(ep)
                episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(ep))
            }
        }
        progress = progress
    }

    private var scrolled = false
    var loadedProgress = false
    var progress: SubjectProgress? = null
        set(value) {
            episodeDetailAdapter.data.forEach { ep ->
                ep.t?.progress = null
                value?.eps?.forEach {
                    if (ep.t?.id == it.id) {
                        ep.t?.progress = it
                    }
                }
            }
            episodeAdapter.notifyDataSetChanged()
            episodeDetailAdapter.notifyDataSetChanged()
            field = value

            if(!scrolled && loadedProgress && episodeAdapter.data.size>0){
                scrolled = true

                var lastView = 0
                episodeAdapter.data.forEachIndexed { index, episode ->
                    if(episode.progress != null)
                        lastView = index
                }
                val layoutManager = (context.episode_list.layoutManager as LinearLayoutManager)
                layoutManager.scrollToPositionWithOffset(lastView, 0)
                layoutManager.stackFromEnd = false
            }
            detail.item_episodes.visibility = if(episodeDetailAdapter.data.isEmpty()) View.GONE else View.VISIBLE
        }

    private fun showEpisodeDetail(show: Boolean){
        context.episode_detail_list_header.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_header.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in else R.anim.move_out)
        context.episode_detail_list.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in else R.anim.move_out)
    }
}