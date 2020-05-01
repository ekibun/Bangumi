package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.app_bar
import kotlinx.android.synthetic.main.activity_subject.root_layout
import kotlinx.android.synthetic.main.dialog_subject.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.topic.PhotoPagerAdapter
import soko.ekibun.bangumi.ui.view.CollapsibleAppBarHelper
import soko.ekibun.bangumi.ui.view.RoundBackgroundDecoration
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.util.TimeUtil

/**
 * 条目view
 * @property context SubjectActivity
 * @property collapsibleAppBarHelper CollapsibleAppBarHelper
 * @property episodeAdapter SmallEpisodeAdapter
 * @property episodeDetailAdapter EpisodeAdapter
 * @property linkedSubjectsAdapter LinkedSubjectAdapter
 * @property recommendSubjectsAdapter LinkedSubjectAdapter
 * @property characterAdapter CharacterAdapter
 * @property tagAdapter TagAdapter
 * @property topicAdapter TopicAdapter
 * @property blogAdapter BlogAdapter
 * @property sitesAdapter SitesAdapter
 * @property commentAdapter CommentAdapter
 * @property seasonAdapter SeasonAdapter
 * @property seasonLayoutManager LinearLayoutManager
 * @property detail View
 * @property isScrollDown Boolean
 * @property onStateChangedListener Function1<Int, Unit>
 * @property peakRatio Float
 * @property peakMargin Float
 * @property insertTop Int
 * @property behavior BottomSheetBehavior<(androidx.constraintlayout.widget.ConstraintLayout..androidx.constraintlayout.widget.ConstraintLayout?)>
 * @property subjectEpisode List<Episode>
 * @property scrolled Boolean
 * @constructor
 */
@SuppressLint("InflateParams")
class SubjectView(private val context: SubjectActivity) {
    val collapsibleAppBarHelper = CollapsibleAppBarHelper(context.app_bar as AppBarLayout)

    val episodeAdapter = SmallEpisodeAdapter()
    val episodeDetailAdapter = EpisodeAdapter()
    val linkedSubjectsAdapter = LinkedSubjectAdapter()
    val recommendSubjectsAdapter = LinkedSubjectAdapter()
    val characterAdapter = CharacterAdapter()
    val tagAdapter = TagAdapter()
    val topicAdapter = TopicAdapter()
    val blogAdapter = BlogAdapter()
    val sitesAdapter = SitesAdapter(collapseLabel = true)
    val commentAdapter = CommentAdapter()
    val seasonAdapter = SeasonAdapter()
    private val seasonLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

    val detail: View by lazy { LayoutInflater.from(context).inflate(R.layout.dialog_subject, null) }

    val isScrollDown
        get() = context.item_list.canScrollVertically(-1) || (behavior.state != BottomSheetBehavior.STATE_COLLAPSED &&
                context.app_bar.height > context.bottom_sheet.paddingTop + context.bottom_sheet.paddingBottom)

    var onStateChangedListener = { state: Int -> }

    fun scroll2Top(): Boolean {
        return if (isScrollDown) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            context.item_list.stopScroll()
            (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            true
        } else false
    }

    var peakRatio = 0f
        set(value) {
            field = value
            behavior.peekHeight =
                context.bottom_sheet.height - Math.max(
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else (context.bottom_sheet.height * value).toInt(),
                    context.app_bar.height - context.bottom_sheet.paddingTop - context.bottom_sheet.paddingBottom
                )
        }

    var peakMargin = 0f
        set(value) {
            field = value
            context.bottom_sheet.let {
                it.setPadding(
                    it.paddingLeft, it.paddingTop, it.paddingRight,
                    insertTop +
                            Math.max(
                                context.toolbar.height,
                                if (behavior.isHideable) 0 else (it.width * value).toInt()
                            )
                )
                it.translationY = it.paddingBottom.toFloat()
            }
        }
    var insertTop = 0
        set(value) {
            field = value
            peakMargin = peakMargin
        }

    val behavior = BottomSheetBehavior.from(context.bottom_sheet)

    init {
        collapsibleAppBarHelper.appbarCollapsible(CollapsibleAppBarHelper.CollapseStatus.EXPANDED)

        context.window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            peakRatio = peakRatio
            peakMargin = peakMargin
        }

        val listPaddingBottom = context.item_list.paddingBottom
        val progressEnd = context.item_swipe.progressViewEndOffset
        var lastInsertBottom = 0
        context.root_layout.setOnApplyWindowInsetsListener { _, insets ->
            insertTop = insets.systemWindowInsetTop
            if (insets.systemWindowInsetBottom < lastInsertBottom) context.currentFocus?.clearFocus()
            lastInsertBottom = insets.systemWindowInsetBottom
            context.item_swipe.setProgressViewEndTarget(false, progressEnd + insets.systemWindowInsetTop)
            if (context.item_swipe.isRefreshing) {
                context.item_swipe.isRefreshing = false
                context.item_swipe.isRefreshing = true
            }
            // episode_detail_list.setPadding(episode_detail_list.paddingLeft, episode_detail_list.paddingTop, episode_detail_list.paddingRight, episodePaddingBottom + insets.systemWindowInsetBottom)
            context.item_list.setPadding(
                context.item_list.paddingLeft,
                context.item_list.paddingTop,
                context.item_list.paddingRight,
                listPaddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }


        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) { /* no-op */
                if (newState == BottomSheetBehavior.STATE_HIDDEN) ThemeModel.fullScreen(context.window)
                else ThemeModel.updateNavigationTheme(context)
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
                if (newState == BottomSheetBehavior.STATE_HIDDEN) context.item_swipe.isRefreshing = false
                onStateChangedListener(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                collapsibleAppBarHelper.updateStatus(slideOffset)
            }
        })

        context.item_mask.setOnClickListener {
            if (behavior.isHideable) behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        context.item_swipe.setOnChildScrollUpCallback { _, _ ->
            isScrollDown || behavior.isHideable
        }

        behavior.isHideable = false

        detail.season_list.adapter = seasonAdapter
        detail.season_list.layoutManager = seasonLayoutManager
        detail.season_list.isNestedScrollingEnabled = false

        detail.episode_list.adapter = episodeAdapter
        detail.episode_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.episode_list.isNestedScrollingEnabled = false

        detail.linked_list.adapter = linkedSubjectsAdapter
        detail.linked_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.linked_list.isNestedScrollingEnabled = false

        detail.commend_list.adapter = recommendSubjectsAdapter
        detail.commend_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.commend_list.isNestedScrollingEnabled = false

        detail.character_list.adapter = characterAdapter
        detail.character_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.character_list.isNestedScrollingEnabled = false

        detail.topic_list.adapter = topicAdapter
        detail.topic_list.layoutManager = LinearLayoutManager(context)
        detail.topic_list.isNestedScrollingEnabled = false

        detail.blog_list.adapter = blogAdapter
        detail.blog_list.layoutManager = LinearLayoutManager(context)
        detail.blog_list.isNestedScrollingEnabled = false

        detail.tag_list.adapter = tagAdapter
        detail.tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.tag_list.isNestedScrollingEnabled = false

        detail.site_list.adapter = sitesAdapter
        detail.site_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        detail.site_list.isNestedScrollingEnabled = false

        context.item_list.adapter = commentAdapter
        context.item_list.addItemDecoration(RoundBackgroundDecoration(ResourceUtil.toPixels(16f)))
        context.item_list.layoutManager = LinearLayoutManager(context)
        commentAdapter.setHeaderView(detail)
    }

    /**
     * 更新条目
     * @param subject Subject
     * @param tag SaxTag?
     */
    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: Subject, tag: Subject.SaxTag? = null) {
        if (context.isDestroyed || tag == Subject.SaxTag.NONE) return

        if (tag == null || tag == Subject.SaxTag.ONAIR) sitesAdapter.setNewInstance(subject.onair?.sites?.toMutableList())

        if ((tag == null || tag == Subject.SaxTag.SEASON) && subject.season?.size ?: 0 > 1) {
            seasonAdapter.setNewInstance(subject.season?.toMutableList())
            seasonAdapter.currentId = subject.id
            seasonLayoutManager.scrollToPositionWithOffset(seasonAdapter.data.indexOfFirst { it.id == subject.id }, 0)
        }

        if (tag == null || tag == Subject.SaxTag.NAME) {
            collapsibleAppBarHelper.setTitle(subject.displayName)
            detail.item_subject_title.text = subject.name
        }
        if (tag == null || tag == Subject.SaxTag.INFOBOX || tag == Subject.SaxTag.NAME) {
            val infoBoxPreview = ArrayList<String>()
            infoBoxPreview.add(if (subject.category.isNullOrEmpty()) context.getString(Subject.getTypeRes(subject.type)) else subject.category!!)
            infoBoxPreview.add(subject.infobox?.firstOrNull { it.first in arrayOf("发售日期", "发售日", "发行日期") }?.let {
                "${Jsoup.parse(it.second).body().text()} ${it.first.substring(0, 2)}"
            } ?: "${subject.air_date ?: ""} ${TimeUtil.weekList[subject.air_weekday]}")

            infoBoxPreview.addAll(subject.infobox?.filter {
                it.first.substringBefore(" ") in arrayOf("动画制作", "作者", "开发", "游戏制作", "艺术家")
            }?.map { "${it.first}：${Jsoup.parse(it.second).body().text()}" } ?: ArrayList())

            infoBoxPreview.addAll(subject.infobox?.filter {
                it.first.substringBefore(" ") in arrayOf("导演", "发行", "出版社", "连载杂志", "作曲", "作词", "编曲", "插图", "作画")
            }?.map { "${it.first}：${Jsoup.parse(it.second).body().text()}" } ?: ArrayList())

            detail.item_subject_info.text = infoBoxPreview.joinToString(" / ")
        }

        if (tag == null || tag == Subject.SaxTag.SUMMARY) {
            detail.item_detail.text = subject.summary
            detail.item_detail.visibility = if (subject.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        if (tag == null || tag == Subject.SaxTag.COLLECTION) {
            detail.item_user_collect.text =
                "${subject.collection?.wish}人想看/${
                subject.collection?.collect}人看过/${
                subject.collection?.doing}人在看/${
                subject.collection?.on_hold}人搁置/${
                subject.collection?.dropped}人抛弃"
            detail.item_user_collect.visibility = if (subject.collection == null) View.INVISIBLE else View.VISIBLE
        }

        if (tag == null || tag == Subject.SaxTag.COLLECT) {
            updateEpisodeLabel(subject.eps ?: ArrayList(), subject)
            detail.item_progress.visibility =
                if (HttpUtil.formhash.isNotEmpty() && subject.collect?.status == Collection.STATUS_DO && subject.type in listOf(
                        Subject.TYPE_ANIME,
                        Subject.TYPE_REAL,
                        Subject.TYPE_BOOK
                    )
                ) View.VISIBLE else View.GONE
            detail.item_vol.visibility = if (subject.vol_count != 0) View.VISIBLE else View.GONE
            detail.item_vol_count.text =
                "${if (subject.vol_count <= 0) "" else "/${subject.vol_count}"} ${context.getString(R.string.vol_unit)}"
            detail.item_vol_status.value = subject.vol_status
            detail.item_ep_count.text =
                "${if (subject.eps_count <= 0) "" else "/${subject.eps_count}"} ${context.getString(R.string.ep_unit)}"
            detail.item_ep_status.value = subject.ep_status
            detail.item_progress_edit.visibility = View.INVISIBLE
            subject.rating?.let {
                detail.detail_score.text = if (it.score == 0f) "-" else String.format("%.1f", it.score)
                detail.detail_friend_score.text =
                    if (it.friend_score == 0f) "-" else String.format("%.1f", it.friend_score)
                detail.detail_score_count.text =
                    "×${if (it.total > 1000) "${it.total / 1000}k" else it.total.toString()}"
                detail.item_friend_score_label.text = context.getString(R.string.friend_score)
            }
        }

        if (tag == null || tag == Subject.SaxTag.IMAGES) {
            GlideUtil.with(detail.item_cover)
                ?.load(Images.getImage(subject.image))
                ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(detail.item_cover.drawable))
                ?.into(detail.item_cover)
            detail.item_cover.setOnClickListener {
                PhotoPagerAdapter.showWindow(
                    detail,
                    listOf(Images.large(subject.image)),
                    listOf(detail.item_cover.drawable)
                )
            }
            GlideUtil.with(context)
                ?.load(Images.getImage(subject.image))
                ?.apply(
                    RequestOptions.bitmapTransform(
                        BlurTransformation(
                            25,
                            8
                        )
                    )
                )?.into(object : CustomTarget<Drawable>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        context.window.setBackgroundDrawable(placeholder)
                    }

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        context.window.setBackgroundDrawable(resource)
                    }
                })
        }

        if (tag == null || tag == Subject.SaxTag.EPISODES)
            subject.eps?.let {
                val eps = updateEpisode(it)
                updateEpisodeLabel(eps, subject)
                subject.eps = eps
            }
        if (tag == null || tag == Subject.SaxTag.TOPIC) {
            detail.item_topics.visibility = if (subject.topic?.isNotEmpty() == true) View.VISIBLE else View.GONE
            topicAdapter.setNewInstance(subject.topic?.toMutableList())
        }
        if (tag == null || tag == Subject.SaxTag.BLOG) {
            detail.item_blogs.visibility = if (subject.blog?.isNotEmpty() == true) View.VISIBLE else View.GONE
            blogAdapter.setNewInstance(subject.blog?.toMutableList())
        }
        if (tag == null || tag == Subject.SaxTag.CHARACTOR) {
            detail.item_character.visibility = if (subject.crt?.isNotEmpty() == true) View.VISIBLE else View.GONE
            characterAdapter.setNewInstance(subject.crt?.toMutableList())
        }
        if (tag == null || tag == Subject.SaxTag.LINKED) {
            detail.item_linked.visibility = if (subject.linked?.isNotEmpty() == true) View.VISIBLE else View.GONE
            linkedSubjectsAdapter.setNewInstance(subject.linked?.toMutableList())
        }
        if (tag == null || tag == Subject.SaxTag.RECOMMEND) {
            detail.item_commend.visibility = if (subject.recommend?.isNotEmpty() == true) View.VISIBLE else View.GONE
            recommendSubjectsAdapter.setNewInstance(subject.recommend?.toMutableList())
        }
        if (tag == null || tag == Subject.SaxTag.TAGS) {
            tagAdapter.setNewInstance(subject.tags?.toMutableList())
            tagAdapter.setOnItemClickListener { _, _, position ->
                WebActivity.startActivity(
                    context,
                    "${Bangumi.SERVER}/${subject.type}/tag/${tagAdapter.data[position].first}"
                )
            }
        }
    }

    /**
     * 更新集数标签
     * @param episodes List<Episode>
     * @param subject Subject
     */
    fun updateEpisodeLabel(episodes: List<Episode>, subject: Subject) {
        val mainEps = episodes.filter { it.type == Episode.TYPE_MAIN || it.type == Episode.TYPE_MUSIC }
        val eps = mainEps.filter { it.isAir }
        detail.episode_detail.text =
            if (eps.size == mainEps.size && (subject.type == Subject.TYPE_MUSIC || subject.eps_count > 0)) context.getString(
                R.string.phrase_full_eps,
                eps.size
            ) else
                eps.lastOrNull()?.parseSort()?.let { context.getString(R.string.parse_update_to, it) }
                    ?: context.getString(R.string.hint_air_nothing)
    }

    private var subjectEpisode: List<Episode> = ArrayList()
    /**
     * 更新剧集列表
     * @param episodes List<Episode>
     * @return List<Episode>
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
        episodeAdapter.setNewInstance(null)
        episodeDetailAdapter.setNewInstance(null)
        maps.forEach {
            episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(it.key))
            it.value.forEach { ep ->
                if (ep.isAir)
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
}