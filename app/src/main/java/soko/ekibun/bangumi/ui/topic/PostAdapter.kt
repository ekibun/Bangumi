package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.BaseNodeAdapter
import soko.ekibun.bangumi.ui.view.FastScrollRecyclerView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HtmlUtil
import soko.ekibun.bangumi.util.span.ClickableUrlSpan
import java.util.*
import kotlin.collections.HashMap

/**
 * 回复 Adapter
 * @constructor
 */
class PostAdapter() :
    BaseNodeAdapter(), FastScrollRecyclerView.SectionedAdapter, LoadMoreModule {

    init {
        addFullSpanNodeProvider(NodeProvider())
    }

    override fun getItemType(data: List<BaseNode>, position: Int): Int {
        return if (data.get(position) is TopicPost) 0 else -1
    }

    override fun getSectionName(position: Int): String {
        val item = (data.getOrNull(position) ?: data.lastOrNull()) as? TopicPost
        return "#${item?.floor}"
    }

    class NodeProvider() : BaseNodeProvider() {
        override val itemViewType: Int = 0
        override val layoutId: Int = R.layout.item_reply

        private val imageSizes = HashMap<String, Size>()
        private val largeContent = WeakHashMap<String, Spanned>()

        @SuppressLint("SetTextI18n")
        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            (getAdapter() as PostAdapter).let {
                it.addOnClickListener(helper, R.id.item_del)
                it.addOnClickListener(helper, R.id.item_reply)
                it.addOnClickListener(helper, R.id.item_dolike)
                it.addOnClickListener(helper, R.id.item_edit)
                it.addOnClickListener(helper, R.id.item_avatar)
            }
            if (item !is TopicPost) return
            // 用户
            helper.itemView.item_user.text =
                if (item.badge.isNullOrEmpty()) item.nickname else "${item.nickname} ${item.pst_content}"
            helper.itemView.item_user_sign.text =
                if (item.badge.isNullOrEmpty()) if (item.sign.length < 2) "" else item.sign.substring(
                    1,
                    item.sign.length - 1
                ) else item.dateline
            val subFloor = if (item.sub_floor > 0) "-${item.sub_floor}" else ""
            helper.itemView.item_time.text = (if (item.floor > 0) "#${item.floor}$subFloor - " else "") + item.dateline
            helper.itemView.item_reply.visibility = if ((item.relate.toIntOrNull() ?: 0) > 0) View.VISIBLE else View.GONE
            helper.itemView.item_del.visibility = if (item.editable) View.VISIBLE else View.GONE
            helper.itemView.item_edit.visibility = helper.itemView.item_del.visibility

            if(helper.itemView.like_list.adapter !is LikeAdapter) {
                helper.itemView.like_list.adapter = LikeAdapter()
                helper.itemView.like_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                helper.itemView.like_list.isNestedScrollingEnabled = false
            }
            (helper.itemView.like_list.adapter as LikeAdapter).let { adapter ->
                adapter.setList(item.likes)
                adapter.setOnItemClickListener { _, _, position ->
                    val like = item.likes?.getOrNull(position)?: return@setOnItemClickListener
                    (context as BaseActivity).subscribe {
                        TopicPost.Like.dolike(item.likeType, like.main_id, item.pst_id, like.value.toString())
                        val user = UserModel.current()?: return@subscribe
                        val newUsers = like.users.toMutableList()
                        if(!newUsers.removeAll { it.username == user.username }) {
                            // 删除其他的，只能留下一个
                            item.likes?.forEach {
                                val newUsersOther = it.users.toMutableList()
                                newUsersOther.removeAll { it.username == user.username }
                                it.total = newUsersOther.size
                                it.users = newUsersOther
                            }
                            UserModel.current()?.let { newUsers.add(it) }
                        }
                        like.total = newUsers.size
                        like.users = newUsers
                        adapter.setList(item.likes)
                    }
                }
            }

            helper.itemView.item_expand.visibility = if (item.children.size > 0) View.VISIBLE else View.GONE
            helper.itemView.item_expand.rotation = if (item.isExpanded) 90f else -90f
            helper.itemView.item_expand.setOnClickListener {
                getAdapter()?.expandOrCollapse(helper.layoutPosition)
            }

            GlideUtil.with(helper.itemView.item_avatar)
                ?.load(Images.small(Bangumi.parseUrl(item.avatar)))
                ?.apply(
                    RequestOptions.circleCropTransform().error(R.drawable.err_404)
                        .placeholder(R.drawable.placeholder_round)
                )
                ?.into(helper.itemView.item_avatar)

            helper.itemView.item_message.visibility = if (item.badge.isNullOrEmpty()) View.VISIBLE else View.GONE
            helper.itemView.item_action_box.visibility = helper.itemView.item_message.visibility
            helper.itemView.offset.text = item.badge
            helper.itemView.offset.visibility = when {
                !item.badge.isNullOrEmpty() -> View.VISIBLE
                item.isSub -> View.INVISIBLE
                else -> View.GONE
            }

            if (item.badge.isNullOrEmpty()) {
                helper.itemView.item_message.let { item_message ->
                    val makeSpan = {
                        HtmlUtil.html2span(item.pst_content,
                            HtmlUtil.ImageGetter(imageSizes) {
                                Math.min(item_message.width.toFloat(), Math.max(item_message.textSize, it))
                            }
                        ).also {
                            it.getSpans(0, it.length, ClickableUrlSpan::class.java).forEach {
                                it.onClick = { v, url ->
                                    (v.context as? TopicActivity)?.processUrl(Bangumi.parseUrl(url))
                                        ?: WebActivity.launchUrl(helper.itemView.context, Bangumi.parseUrl(url), "")
                                }
                            }
                        }
                    }
                    HtmlUtil.attachToTextView(
                        largeContent.getOrPut(item.pst_id + "_" + item.pst_content, makeSpan),
                        item_message
                    )
                }
                helper.itemView.item_message.onFocusChangeListener = View.OnFocusChangeListener { view, focus ->
                    if (!focus) {
                        view.tag = null
                        (view as TextView).text = view.text
                    }
                }
                helper.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }


    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)

        // Bug workaround for losing text selection ability, see:
        // https://code.google.com/p/android/issues/detail?id=208169
        holder.itemView.item_message?.isEnabled = false
        holder.itemView.item_message?.isEnabled = true
    }
}