package soko.ekibun.bangumi.ui.topic

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_topic.app_bar
import kotlinx.android.synthetic.main.activity_topic.btn_reply
import kotlinx.android.synthetic.main.activity_topic.item_list
import kotlinx.android.synthetic.main.activity_topic.item_swipe
import kotlinx.android.synthetic.main.appbar_layout.title_slice_0
import kotlinx.android.synthetic.main.appbar_layout.title_slice_1
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.CollapsibleAppBarHelper
import soko.ekibun.bangumi.ui.view.DimBlurTransform
import soko.ekibun.bangumi.ui.view.RoundBackgroundDecoration
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil


/**
 * 帖子View
 * @property context TopicActivity
 * @property collapsibleAppBarHelper CollapsibleAppBarHelper
 * @property adapter PostAdapter
 * @property scroll2Top Function0<Boolean>
 * @constructor
 */
class TopicView(private val context: TopicActivity) {
    private val collapsibleAppBarHelper = CollapsibleAppBarHelper(context.app_bar as AppBarLayout)

    val adapter by lazy { PostAdapter() }

    private val scroll2Top = {
        if (context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0) {
            collapsibleAppBarHelper.appbar.setExpanded(true, true)
            context.item_list.stopScroll()
            context.item_list.layoutManager?.scrollToPosition(0)
            true
        } else false
    }

    init {
        context.item_list.adapter = adapter
        context.item_list.layoutManager = object : LinearLayoutManager(context) {
            override fun getFocusedChild(): View? {
                return null
            }

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
        adapter.setEmptyView(LayoutInflater.from(context).inflate(R.layout.view_empty, context.item_list, false))
        adapter.isUseEmpty = false

        var canScroll = false
        collapsibleAppBarHelper.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, _ ->
            canScroll = canScroll || collapsibleAppBarHelper.appBarOffset != 0
            context.item_list.invalidate()
        })
        context.item_list.nestedScrollRange = {
            collapsibleAppBarHelper.appbar.totalScrollRange
        }
        context.item_list.nestedScrollDistance = {
            -collapsibleAppBarHelper.appBarOffset
        }

        context.item_list.addItemDecoration(RoundBackgroundDecoration())

        context.item_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                canScroll = context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0
                context.item_swipe.setOnChildScrollUpCallback { _, _ -> canScroll }
            }
        })
        collapsibleAppBarHelper.onTitleClickListener = {
            if (!scroll2Top()) WebActivity.startActivity(context, context.topicPresenter.topic.url)
        }
    }

    fun scrollToPost(scrollPost: String, smooth: Boolean = false) {
        (context.item_list?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
            val scrollIndex = adapter.data.indexOfFirst { (it as? TopicPost)?.pst_id == scrollPost }
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
     * @param topic Topic
     * @param scrollPost String
     * @param header Boolean
     * @param isCache Boolean
     * @param onItemClick Function2<View, Int, Unit>
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
        Log.v("TOPIC", Images.getImage(topic.image.toString()))
        GlideUtil.with(context)
            ?.load(Images.getImage(topic.image))
            ?.apply(
                RequestOptions.bitmapTransform(
                    DimBlurTransform(
                        25,
                        8,
                        100
                    )
                )
            )
            ?.into(object : CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    placeholder?.let { context.window.setBackgroundDrawable(it) }
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    context.window.setBackgroundDrawable(resource)
                }
            })

        adapter.setOnItemChildClickListener { _, v, position ->
            onItemClick(v, position)
        }

        if (header) return
        adapter.isUseEmpty = !isCache
        setNewData(topic, isCache)
        scrollToPost(scrollPost)
        adapter.loadMoreModule.loadMoreEnd()

        if (isCache) return

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
    }

    private fun setNewData(topic: Topic, isCache: Boolean = false) {
        // 加上blog的正文
        adapter.setNewInstance(listOfNotNull(topic.blog).plus(topic.replies).toMutableList())
        if (!isCache) context.topicPresenter.dataCacheModel.set(topic.cacheKey, topic)
    }

}