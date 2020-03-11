package soko.ekibun.bangumi.ui.say

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_topic.*
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.ui.view.CollapsibleAppBarHelper
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil


class SayView(private val context: SayActivity) {
    private val collapsibleAppBarHelper = CollapsibleAppBarHelper(context.app_bar as AppBarLayout)

    val adapter by lazy { SayAdapter() }

    private val scroll2Top = {
        if (context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0) {
            collapsibleAppBarHelper.appbar.setExpanded(true, true)
            context.item_list.stopScroll()
            (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            true
        } else false
    }

    init {
        adapter.setUpWithRecyclerView(context.shc, context.item_list)
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

        context.item_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                canScroll = context.item_list.canScrollVertically(-1) || collapsibleAppBarHelper.appBarOffset != 0
                context.item_swipe.setOnChildScrollUpCallback { _, _ -> canScroll }
            }
        })
        context.title_collapse.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            WebActivity.startActivity(context, context.sayPresenter.say.url)
        }
        context.title_expand.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            WebActivity.startActivity(context, context.sayPresenter.say.url)
        }
    }

    fun processSay(say: Say, header: Boolean = false, isCache: Boolean = false) {
        collapsibleAppBarHelper.setTitle("吐槽", say.user.name, say.time)
        context.title_slice_0.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            WebActivity.launchUrl(context, say.user.url, "")
        }

        GlideUtil.with(context.item_cover_blur)
            ?.load(Images.getImage(say.user.avatar, context))
            ?.apply(
                RequestOptions.bitmapTransform(
                    BlurTransformation(
                        25,
                        8
                    )
                ).placeholder(context.item_cover_blur.drawable)
            )
            ?.into(context.item_cover_blur)

        if (header) return

        adapter.isUseEmpty(true)
        adapter.setNewData(say)
        adapter.loadMoreEnd()

        if (isCache) return

        context.btn_reply.text = when {
            say.self != null -> context.getString(R.string.hint_reply)
            else -> context.getString(R.string.hint_login_topic)
        }
        context.btn_reply.setCompoundDrawablesWithIntrinsicBounds(
            if (say.self != null) ResourceUtil.getDrawable(context, R.drawable.ic_edit)
            else null,//left
            null,
            if (say.self != null) ResourceUtil.getDrawable(context, R.drawable.ic_send)
            else null,//right
            null
        )
    }
}