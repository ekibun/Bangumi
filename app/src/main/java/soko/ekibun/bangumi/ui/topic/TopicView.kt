package soko.ekibun.bangumi.ui.topic

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_topic.*
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.CollapsibleAppBarHelper
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil


/**
 * 帖子View
 */
class TopicView(private val context: TopicActivity) {
    private val collapsibleAppBarHelper = CollapsibleAppBarHelper(context.app_bar as AppBarLayout)

    val adapter by lazy { PostAdapter() }

    private val scroll2Top = {
        if (context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0) {
            collapsibleAppBarHelper.appbar.setExpanded(true, true)
            context.item_list.stopScroll()
            (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            true
        } else false
    }

    init {
        context.item_list.adapter = adapter
        context.item_list.layoutManager = object : LinearLayoutManager(context) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean
            ): Boolean {
                return false
            }

            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                return false
            }
        }
        adapter.emptyView = LayoutInflater.from(context).inflate(R.layout.view_empty, context.item_list, false)
        adapter.isUseEmpty(false)
        adapter.setEnableLoadMore(true)

        var canScroll = false
        collapsibleAppBarHelper.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            canScroll = canScroll || collapsibleAppBarHelper.appBarOffset != 0
            context.item_list.invalidate()
        })
        context.item_list.nestedScrollRange = {
            collapsibleAppBarHelper.appbar.totalScrollRange
        }
        context.item_list.nestedScrollDistance = {
            -collapsibleAppBarHelper.appBarOffset
        }

        val maxBgOffset = ResourceUtil.toPixels(context.resources, 24f)
        context.item_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                canScroll = context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0
                context.item_swipe.setOnChildScrollUpCallback { _, _ -> canScroll }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
//                context.item_bg.translationY =
//                    -Math.min(maxBgOffset, context.item_list.getScrolledPastHeight()).toFloat()
            }
        })
        context.title_collapse.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            WebActivity.startActivity(context, context.topicPresenter.topic.url)
        }
        context.title_expand.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            WebActivity.startActivity(context, context.topicPresenter.topic.url)
        }
    }

    fun scrollToPost(scrollPost: String, smooth: Boolean = false) {
        (context.item_list?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
            val scrollIndex = adapter.data.indexOfFirst { it.pst_id == scrollPost }
            if (scrollIndex <= 0) return@let
            if (smooth) {
                if (scrollIndex + 1 < layoutManager.findFirstVisibleItemPosition())
                    layoutManager.scrollToPositionWithOffset(scrollIndex, -(context.item_list?.height ?: 0))
                if (scrollIndex - 1 > layoutManager.findLastVisibleItemPosition())
                    layoutManager.scrollToPositionWithOffset(scrollIndex, context.item_list?.height ?: 0)
                context.item_list?.post {
                    layoutManager.startSmoothScroll(object : LinearSmoothScroller(context) {
                        init {
                            targetPosition = scrollIndex
                        }

                        override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                    })
                }
            } else layoutManager.scrollToPositionWithOffset(scrollIndex, 0)
        }
    }

    /**
     * 处理帖子内容
     */
    fun processTopic(
        topic: Topic,
        scrollPost: String,
        header: Boolean = false,
        isCache: Boolean = false,
        onItemClick: (View, Int) -> Unit = { _, _ -> }
    ) {
        collapsibleAppBarHelper.setTitle(topic.title ?: "", topic.links?.toList()?.getOrNull(0)?.let { link ->
            context.title_slice_0.setOnClickListener {
                if (scroll2Top()) return@setOnClickListener
                WebActivity.launchUrl(context, link.second, topic.url)
            }
            link.first
        }, topic.links?.toList()?.getOrNull(1)?.let { link ->
            context.title_slice_1.setOnClickListener {
                if (scroll2Top()) return@setOnClickListener
                WebActivity.launchUrl(context, link.second, topic.url)
            }
            link.first
        })

        GlideUtil.with(context.item_cover_blur)
                ?.load(Images.getImage(topic.image, context))
                ?.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)).placeholder(context.item_cover_blur.drawable))
                ?.into(context.item_cover_blur)

        if (header) return
        adapter.isUseEmpty(!isCache)
        topic.replies.forEach { it.isExpanded = true }
        setNewData(topic.replies, topic, isCache)
        scrollToPost(scrollPost)

        adapter.loadMoreEnd()
        val lastPost = topic.replies.lastOrNull()
        context.btn_reply.text = when {
            !topic.lastview.isNullOrEmpty() -> context.getString(R.string.hint_reply)
            topic.error != null -> topic.error?.first
            topic.replies.isEmpty() -> context.getString(R.string.hint_empty_topic)
            !lastPost?.badge.isNullOrEmpty() -> context.getString(R.string.hint_closed_topic)
            else -> context.getString(R.string.hint_login_topic)
        }
        context.btn_reply.setCompoundDrawablesWithIntrinsicBounds(
                when {
                    !topic.lastview.isNullOrEmpty() -> ResourceUtil.getDrawable(context, R.drawable.ic_edit)
                    topic.replies.isNotEmpty() -> ResourceUtil.getDrawable(context, R.drawable.ic_lock)
                    else -> null
                },//left
                null,
                if (!topic.lastview.isNullOrEmpty()) ResourceUtil.getDrawable(context, R.drawable.ic_send) else null,//right
                null
        )

        adapter.setOnItemChildClickListener { _, v, position ->
            onItemClick(v, position)
        }
    }

    /**
     * 更新楼层数据
     */
    fun setNewData(data: List<TopicPost>, topic: Topic, isCache: Boolean = false) {
        var floor = 0
        var subFloor = 0
        var referPost: TopicPost? = null
        // 楼层排序
        val replies = data.filter {
            if (it.isSub) {
                subFloor++
            } else {
                floor++
                subFloor = 0
            }
            it.floor = floor
            it.sub_floor = subFloor
            it.editable = it.is_self
            if (subFloor == 0) {
                referPost = it
                referPost?.subItems?.clear()
                true
            } else {
                referPost?.editable = false
                referPost?.addSubItem(it)
                false
            }
        }
        // 加上blog的正文
        adapter.setNewData(listOfNotNull(topic.blog).plus(replies))
        var i = 0
        while (i < adapter.data.size) {
            val topicPost = adapter.data[i]
            if (topicPost.isExpanded) {
                topicPost.isExpanded = false
                adapter.expand(i, false, false)
            }
            i++
        }
        // 缓存
        topic.replies = data
        if (!isCache) context.topicPresenter.dataCacheModel.set(topic.cacheKey, topic)
    }
}