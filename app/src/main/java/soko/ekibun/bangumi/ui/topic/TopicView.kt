package soko.ekibun.bangumi.ui.topic

import android.graphics.Rect
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI

class TopicView(private val context: TopicActivity){
    val adapter by lazy { PostAdapter() }

    init{
        context.item_list.adapter = adapter
        context.item_list.layoutManager = object: LinearLayoutManager(context){
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean): Boolean { return false }
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean, focusedChildVisible: Boolean): Boolean { return false }
        }
        adapter.emptyView = LayoutInflater.from(context).inflate(R.layout.view_empty, context.item_list, false)
        adapter.isUseEmpty(false)

        var appBarOffset = 0
        var canScroll = false
        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ appBarLayout, verticalOffset ->
            val ratio = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            context.title_collapse.alpha = 1-(1-ratio)*(1-ratio)*(1-ratio)
            context.title_expand.alpha = 1-ratio
            context.title_collapse.translationY = -context.title_slice.height / 2 * ratio
            context.title_expand.translationY = context.title_collapse.translationY
            context.title_slice.translationY =  (context.title_collapse.height - context.title_expand.height - (context.title_slice.layoutParams as ConstraintLayout.LayoutParams).topMargin - context.title_slice.height / 2) * ratio

            appBarOffset = verticalOffset
            canScroll = canScroll || appBarOffset != 0
        })

        context.item_list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                canScroll = context.item_list.canScrollVertically(-1) || appBarOffset != 0
                context.item_swipe.setOnChildScrollUpCallback { _, _ -> canScroll }
            }
        })
    }

    fun processTopic(topic: Topic, scrollPost: String, onItemClick: (View, Int)->Unit){
        context.title_collapse.text = topic.title
        context.title_expand.text = context.title_collapse.text
        context.title_collapse.setOnClickListener {
            WebActivity.launchUrl(context, context.openUrl)
        }
        context.title_expand.setOnClickListener {
            WebActivity.launchUrl(context, context.openUrl)
        }
        topic.links.toList().getOrNull(0)?.let{link ->
            context.title_slice_0.text = link.first
            context.title_slice_0.setOnClickListener {
                WebActivity.launchUrl(context, link.second, context.openUrl)
            }
        }
        topic.links.toList().getOrNull(1)?.let{link ->
            context.title_slice_1.text = link.first
            context.title_slice_1.setOnClickListener {
                WebActivity.launchUrl(context, link.second, context.openUrl)
            }
        }
        context.title_slice_divider.visibility = if(context.title_slice_1.text.isNotEmpty()) View.VISIBLE else View.GONE
        context.title_slice_1.visibility = context.title_slice_divider.visibility
        context.title_slice_0.post{
            context.title_slice_0.maxWidth = context.title_expand.width - if(context.title_slice_divider.visibility == View.VISIBLE) 2*context.title_slice_divider.width + context.title_slice_1.width else 0
        }

        if(!topic.replies.isEmpty())
            GlideUtil.with(context.item_cover_blur)
                    ?.load(HttpUtil.getUrl(topic.replies.firstOrNull()?.avatar?:"", URI.create(Bangumi.SERVER)))
                    ?.apply(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                    ?.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                    ?.into(context.item_cover_blur)
        adapter.isUseEmpty(true)
        topic.replies.forEach { it.isExpanded = true }
        setNewData(topic.replies)
        (context.item_list?.layoutManager as? LinearLayoutManager)?.let{ layoutManager ->
            layoutManager.scrollToPositionWithOffset(adapter.data.indexOfFirst { it.pst_id == scrollPost }, 0) }
        adapter.setOnLoadMoreListener({adapter.loadMoreEnd()}, context.item_list)
        adapter.setEnableLoadMore(true)
        context.item_reply.text = when {
            !topic.formhash.isNullOrEmpty() -> context.getString(R.string.hint_reply)
            !topic.error.isNullOrEmpty() -> topic.error
            topic.replies.isEmpty() -> context.getString(R.string.hint_empty_topic)
            else -> context.getString(R.string.hint_login_topic)
        }

        adapter.setOnItemChildClickListener { _, v, position ->
            onItemClick(v, position)
        }
    }


    fun setNewData(data: List<TopicPost>){
        var floor = 0
        var subFloor = 0
        var referPost: TopicPost? = null

        adapter.setNewData(data.filter {
            if(it.isSub){
                subFloor++
            }else{
                floor++
                subFloor=0 }
            it.floor = floor
            it.sub_floor = subFloor
            it.editable = it.is_self
            if(subFloor == 0) {
                referPost = it
                referPost?.subItems?.clear()
                true
            }
            else {
                referPost?.editable = false
                referPost?.addSubItem(it)
                false
            }
        })
        var i = 0
        while(i< adapter.data.size){
            val topicPost = adapter.data[i]
            if(topicPost.isExpanded){
                topicPost.isExpanded = false
                adapter.expand(i, false, false)
            }
            i++
        }
        //adapter.expandAll()
    }
}