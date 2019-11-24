package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import kotlinx.android.synthetic.main.subject_panel.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.ui.topic.PhotoPagerAdapter
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.PlayerBridge
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 条目view
 */
class SubjectView(private val context: SubjectActivity) {

    val episodeAdapter = SmallEpisodeAdapter()
    val episodeDetailAdapter = EpisodeAdapter()
    val linkedSubjectsAdapter = LinkedSubjectAdapter()
    val recommendSubjectsAdapter = LinkedSubjectAdapter()
    val characterAdapter = CharacterAdapter()
    val tagAdapter = TagAdapter()
    val topicAdapter = TopicAdapter()
    val blogAdapter = BlogAdapter()
    val sitesAdapter = SitesAdapter()
    val commentAdapter = CommentAdapter()
    val seasonAdapter = SeasonAdapter()
    private val seasonLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

    val detail = context.subject_detail as LinearLayout

    var appBarOffset = -1
    val scroll2Top = {
        if (appBarOffset != 0 || if (context.episode_detail_list.visibility == View.VISIBLE) context.episode_detail_list.canScrollVertically(-1) else context.comment_list.canScrollVertically(-1)) {
            context.app_bar.setExpanded(true, true)
            context.comment_list.stopScroll()
            (context.comment_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            context.episode_detail_list.stopScroll()
            (context.episode_detail_list.layoutManager as StaggeredGridLayoutManager).scrollToPositionWithOffset(0, 0)
            true
        } else false
    }

    init {
        val marginEnd = ResourceUtil.toPixels(context.resources, 12f)
        val dp20 = ResourceUtil.toPixels(context.resources, 20f)
        (context.title_expand.layoutParams as ConstraintLayout.LayoutParams).marginEnd = 3 * marginEnd

        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (appBarOffset == verticalOffset) return@OnOffsetChangedListener
            val ratio = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            appBarOffset = verticalOffset
            context.item_scrim.alpha = ratio
            context.item_subject.alpha = 1 - ratio
            context.item_subject.translationY = -(4 + ratio * 0.8f) * dp20

            context.item_buttons.translationY = -context.toolbar.height * ratio / 2
            context.title_collapse.alpha = 1 - (1 - ratio) * (1 - ratio) * (1 - ratio)
            context.title_expand.alpha = 1 - ratio
            context.item_buttons.translationX = -2.2f * marginEnd * ratio
            context.app_bar.elevation = Math.max(0f, 12 * (ratio - 0.95f) / 0.05f)

            detail.setPadding(0, ((1 - ratio) * dp20).toInt(), 0, 0)
            context.episode_detail_list_container.setPadding(0, ((1 - ratio) * dp20).toInt(), 0, 0)
        })

        context.season_list.adapter = seasonAdapter
        context.season_list.layoutManager = seasonLayoutManager
        context.season_list.isNestedScrollingEnabled = false

        context.episode_list.adapter = episodeAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        context.episode_list.layoutManager = layoutManager
        context.episode_list.isNestedScrollingEnabled = false
        val swipeTouchListener = View.OnTouchListener { v, _ ->
            if ((v as? RecyclerView)?.canScrollHorizontally(1) == true || (v as? RecyclerView)?.canScrollHorizontally(-1) == true)
                context.shouldCancelActivity = false
            false
        }
        context.episode_list.setOnTouchListener(swipeTouchListener)
        context.season_list.setOnTouchListener(swipeTouchListener)
        context.commend_list.setOnTouchListener(swipeTouchListener)
        context.linked_list.setOnTouchListener(swipeTouchListener)
        context.character_list.setOnTouchListener(swipeTouchListener)
        context.tag_list.setOnTouchListener(swipeTouchListener)
        context.site_list.setOnTouchListener(swipeTouchListener)

        val touchListener = episodeDetailAdapter.setUpWithRecyclerView(context.episode_detail_list)
        touchListener.nestScrollDistance = {
            context.app_bar.totalScrollRange + appBarOffset
        }
        context.episode_detail_list.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        context.episode_detail_list.nestedScrollDistance = {
            -appBarOffset * (context.app_bar.totalScrollRange + dp20) / context.app_bar.totalScrollRange
        }
        context.episode_detail_list.nestedScrollRange = {
            context.app_bar.totalScrollRange + dp20
        }

        context.item_close.setOnClickListener {
            closeEpisodeDetail()
        }
        context.episode_detail.setOnClickListener {
            showEpisodeDetail(true)
        }

        context.linked_list.adapter = linkedSubjectsAdapter
        context.linked_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.linked_list.isNestedScrollingEnabled = false

        context.commend_list.adapter = recommendSubjectsAdapter
        context.commend_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.commend_list.isNestedScrollingEnabled = false

        context.character_list.adapter = characterAdapter
        context.character_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.character_list.isNestedScrollingEnabled = false

        context.topic_list.adapter = topicAdapter
        context.topic_list.layoutManager = LinearLayoutManager(context)
        context.topic_list.isNestedScrollingEnabled = false

        context.blog_list.adapter = blogAdapter
        context.blog_list.layoutManager = LinearLayoutManager(context)
        context.blog_list.isNestedScrollingEnabled = false

        context.tag_list.adapter = tagAdapter
        context.tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.tag_list.isNestedScrollingEnabled = false

        context.site_list.adapter = sitesAdapter
        context.site_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.site_list.isNestedScrollingEnabled = false

        context.comment_list.adapter = commentAdapter
        context.comment_list.layoutManager = LinearLayoutManager(context)

        context.root_layout.removeView(detail)
        commentAdapter.setHeaderView(detail)
    }

    /**
     * 收起剧集列表
     */
    fun closeEpisodeDetail() {
        val eps = episodeDetailAdapter.data.filter { it.isSelected }
        if (eps.isEmpty())
            showEpisodeDetail(false)
        else {
            for (ep in eps) ep.isSelected = false
            episodeDetailAdapter.updateSelection()
            episodeDetailAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 更新条目
     */
    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: Subject, tag: Subject.SaxTag? = null) {
        if (context.isDestroyed || tag == Subject.SaxTag.NONE) return

        if (tag == null || tag == Subject.SaxTag.ONAIR) sitesAdapter.setNewData(subject.onair?.sites)

        if ((tag == null || tag == Subject.SaxTag.SEASON) && subject.season?.size ?: 0 > 1) {
            seasonAdapter.setNewData(subject.season)
            seasonAdapter.currentId = subject.id
            seasonLayoutManager.scrollToPositionWithOffset(seasonAdapter.data.indexOfFirst { it.id == subject.id }, 0)
        }

        if (tag == null || tag == Subject.SaxTag.NAME) {
            context.title_collapse.text = subject.displayName
            context.title_expand.text = context.title_collapse.text
            context.title_collapse.setPadding(context.title_collapse.paddingLeft, context.title_collapse.paddingTop, context.item_buttons.width, context.title_collapse.paddingBottom)
            context.item_subject_title.text = subject.name
        }
        if (tag == null || tag == Subject.SaxTag.INFOBOX || tag == Subject.SaxTag.NAME) {
            val infoBoxPreview = ArrayList<String>()
            infoBoxPreview.add(if (subject.category.isNullOrEmpty()) context.getString(Subject.getTypeRes(subject.type)) else subject.category!!)
            infoBoxPreview.add(subject.infobox?.firstOrNull { it.first in arrayOf("发售日期", "发售日", "发行日期") }?.let {
                "${Jsoup.parse(it.second).body().text()} ${it.first.substring(0, 2)}"
            } ?: "${subject.air_date ?: ""} ${CalendarAdapter.weekList[subject.air_weekday]}")

            infoBoxPreview.addAll(subject.infobox?.filter {
                it.first.substringBefore(" ") in arrayOf("动画制作", "作者", "开发", "游戏制作", "艺术家")
            }?.map { "${it.first}：${Jsoup.parse(it.second).body().text()}" } ?: ArrayList())

            infoBoxPreview.addAll(subject.infobox?.filter {
                it.first.substringBefore(" ") in arrayOf("导演", "发行", "出版社", "连载杂志", "作曲", "作词", "编曲", "插图", "作画")
            }?.map { "${it.first}：${Jsoup.parse(it.second).body().text()}" } ?: ArrayList())

            context.item_subject_info.text = infoBoxPreview.joinToString(" / ")
        }

        if (tag == null || tag == Subject.SaxTag.SUMMARY) {
            detail.item_detail.text = subject.summary
            detail.item_detail.visibility = if (subject.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        if (tag == null || tag == Subject.SaxTag.TYPE) {
            context.item_play.visibility = if (PlayerBridge.checkActivity(context, subject)) View.VISIBLE else View.GONE
        }

        if (tag == null || tag == Subject.SaxTag.COLLECT) {
            updateEpisodeLabel(subject.eps ?: ArrayList(), subject)
            detail.item_progress.visibility = if (HttpUtil.formhash.isNotEmpty() && subject.collect?.status == Collection.STATUS_DO && subject.type in listOf(Subject.TYPE_ANIME, Subject.TYPE_REAL, Subject.TYPE_BOOK)) View.VISIBLE else View.GONE
            detail.item_progress_info.text = context.getString(R.string.phrase_progress,
                    (if (subject.vol_count != 0) context.getString(R.string.parse_sort_vol, "${subject.vol_status}${if (subject.vol_count == 0) "" else "/${subject.vol_count}"}") + " " else "") +
                            context.getString(R.string.parse_sort_ep, "${subject.ep_status}${if (subject.eps_count == 0) "" else "/${subject.eps_count}"}"))
            subject.rating?.let {
                context.detail_score.text = if (it.score == 0f) "-" else String.format("%.1f", it.score)
                context.detail_friend_score.text = if (it.friend_score == 0f) "-" else String.format("%.1f", it.friend_score)
                context.detail_score_count.text = "×${if (it.total > 1000) "${it.total / 1000}k" else it.total.toString()}"
                context.item_friend_score_label.text = context.getString(R.string.friend_score)
            }
        }

        if (tag == null || tag == Subject.SaxTag.IMAGES) {
            GlideUtil.with(context.item_cover)
                    ?.load(Images.getImage(subject.image, context))
                    ?.apply(RequestOptions.placeholderOf(context.item_cover.drawable))
                    ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                    ?.into(context.item_cover)
            context.item_cover.setOnClickListener {
                PhotoPagerAdapter.showWindow(detail, listOf(Images.large(subject.image)), listOf(context.item_cover.drawable))
            }
            GlideUtil.with(context.item_cover_blur)
                    ?.load(Images.getImage(subject.image, context))
                    ?.apply(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                    ?.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                    ?.into(context.item_cover_blur)
        }

        if (tag == null || tag == Subject.SaxTag.EPISODES)
            subject.eps?.let {
                val eps = updateEpisode(it)
                updateEpisodeLabel(eps, subject)
                subject.eps = eps
            }
        if (tag == null || tag == Subject.SaxTag.TOPIC) {
            detail.item_topics.visibility = if (subject.topic?.isNotEmpty() == true) View.VISIBLE else View.GONE
            topicAdapter.setNewData(subject.topic)
        }
        if (tag == null || tag == Subject.SaxTag.BLOG) {
            detail.item_blogs.visibility = if (subject.blog?.isNotEmpty() == true) View.VISIBLE else View.GONE
            blogAdapter.setNewData(subject.blog)
        }
        if (tag == null || tag == Subject.SaxTag.CHARACTOR) {
            detail.item_character.visibility = if (subject.crt?.isNotEmpty() == true) View.VISIBLE else View.GONE
            characterAdapter.setNewData(subject.crt)
        }
        if (tag == null || tag == Subject.SaxTag.LINKED) {
            detail.item_linked.visibility = if (subject.linked?.isNotEmpty() == true) View.VISIBLE else View.GONE
            linkedSubjectsAdapter.setNewData(subject.linked)
        }
        if (tag == null || tag == Subject.SaxTag.RECOMMEND) {
            detail.item_commend.visibility = if (subject.recommend?.isNotEmpty() == true) View.VISIBLE else View.GONE
            recommendSubjectsAdapter.setNewData(subject.recommend)
        }
        if (tag == null || tag == Subject.SaxTag.TAGS) {
            tagAdapter.setNewData(subject.tags?.toMutableList())
            tagAdapter.setOnItemClickListener { _, _, position ->
                WebActivity.launchUrl(context, "${Bangumi.SERVER}/${subject.type}/tag/${tagAdapter.data[position].first}")
            }
        }
    }

    /**
     * 更新集数标签
     */
    fun updateEpisodeLabel(episodes: List<Episode>, subject: Subject) {
        val mainEps = episodes.filter { it.type == Episode.TYPE_MAIN || it.type == Episode.TYPE_MUSIC }
        val eps = mainEps.filter { it.status in listOf(Episode.STATUS_AIR) }
        detail.episode_detail.text = if (eps.size == mainEps.size && (subject.type == Subject.TYPE_MUSIC || subject.eps_count > 0)) context.getString(R.string.phrase_full_eps, eps.size) else
            eps.lastOrNull()?.parseSort(context)?.let { context.getString(R.string.parse_update_to, it) }
                    ?: context.getString(R.string.hint_air_nothing)
    }

    private var subjectEpisode: List<Episode> = ArrayList()
    /**
     * 更新剧集列表
     */
    fun updateEpisode(episodes: List<Episode>): List<Episode> {
        if (episodes.none { it.id != 0 }) return subjectEpisode
        episodes.forEach { ep ->
            ep.merge(subjectEpisode.firstOrNull { it.id == ep.id } ?: return@forEach)
        }
        subjectEpisode = episodes.plus(subjectEpisode).distinctBy { it.id }.sortedBy { it.sort }
        val maps = LinkedHashMap<String, List<Episode>>()
        subjectEpisode.forEach {
            val key = it.category ?: context.getString(Episode.getTypeRes(it.type))
            maps[key] = (maps[key] ?: ArrayList()).plus(it)
        }
        val lastEpisodeSize = episodeDetailAdapter.data.size
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
            episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(true, it.key))
            it.value.forEach { ep ->
                if (ep.status in listOf(Episode.STATUS_AIR))
                    episodeAdapter.addData(ep)
                episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(ep))
            }
        }
        if ((!scrolled || episodeDetailAdapter.data.size != lastEpisodeSize) && episodeAdapter.data.any { it.progress != null }) {
            scrolled = true

            var lastView = 0
            episodeAdapter.data.forEachIndexed { index, episode ->
                if (episode.progress in arrayOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_DROP, Episode.PROGRESS_QUEUE))
                    lastView = index
            }
            val layoutManager = (detail.episode_list.layoutManager as LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(lastView, 0)
            layoutManager.stackFromEnd = false
        }
        detail.item_episodes.visibility = if (episodeDetailAdapter.data.isEmpty()) View.GONE else View.VISIBLE

        return subjectEpisode
    }

    private var scrolled = false

    private fun showEpisodeDetail(show: Boolean) {
        context.episode_detail_list_container.visibility = if (show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_container.animation = AnimationUtils.loadAnimation(context, if (show) R.anim.move_in else R.anim.move_out)
    }
}