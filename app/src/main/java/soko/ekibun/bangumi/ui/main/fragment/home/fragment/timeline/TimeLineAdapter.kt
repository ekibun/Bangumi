package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.like_list
import kotlinx.android.synthetic.main.item_timeline.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.say.SayActivity
import soko.ekibun.bangumi.ui.topic.EmojiAdapter
import soko.ekibun.bangumi.ui.topic.LikeAdapter
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HtmlUtil

/**
 * 时间线Adapter
 * @constructor
 */
class TimeLineAdapter(data: MutableList<TimeLine>? = null) :
    BaseSectionQuickAdapter<TimeLine, BaseViewHolder>
        (R.layout.item_episode_header, R.layout.item_timeline, data), LoadMoreModule {
    override fun convertHeader(helper: BaseViewHolder, item: TimeLine) {
        helper.setText(R.id.item_header, item.header)
    }

    val likePopup by lazy {
        val likePopupView = RecyclerView(context)
        likePopupView.layoutManager = GridLayoutManager(context, 4)
        val likeEmojis = TopicPost.Like.emojiWrap.map { it.key.toString() to Bangumi.parseUrl(it.value) }.toMutableList()
        val adapter = EmojiAdapter(likeEmojis)
        likePopupView.adapter = adapter
        val popupWindow = PopupWindow(likePopupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,true)
        popupWindow.setBackgroundDrawable(
            AppCompatResources.getDrawable(context, R.drawable.abc_popup_background_mtrl_mult)
        )
        popupWindow.isOutsideTouchable = true
        { view: View, cb: (String)->Unit ->
            popupWindow.showAsDropDown(view)
            adapter.setOnItemChildClickListener { _, _, position ->
                popupWindow.dismiss()
                cb(likeEmojis[position].first)
            }
        }
    }

    override fun convert(holder: BaseViewHolder, item: TimeLine) {
        //say
        holder.itemView.item_layout.setOnClickListener {
            SayActivity.startActivity(holder.itemView.context, item.t?.say ?: return@setOnClickListener)
        }
        if(holder.itemView.like_list.adapter !is LikeAdapter) {
            holder.itemView.like_list.adapter = LikeAdapter()
            holder.itemView.like_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            holder.itemView.like_list.isNestedScrollingEnabled = false
            ShadowDecoration.set(holder.itemView.like_list, drawEnd = true)
        }
        (holder.itemView.like_list.adapter as LikeAdapter).let { adapter ->
            adapter.setList(item.t?.say?.likes)
            adapter.setOnItemClickListener { _, _, position ->
                val say = item.t?.say?: return@setOnItemClickListener
                val like = say.likes?.getOrNull(position)?: return@setOnItemClickListener
                (holder.itemView.context as BaseActivity).subscribe {
                    TopicPost.Like.dolike(50, 1, say.id.toString(), like.value.toString())
                    val user = UserModel.current()?: return@subscribe
                    val newUsers = like.users.toMutableList()
                    if(!newUsers.removeAll { it.username == user.username }){
                        // 删除其他的，只能留下一个
                        say.likes?.forEach {
                            val newUsersOther = it.users.toMutableList()
                            newUsersOther.removeAll { it.username == user.username }
                            it.total = newUsersOther.size
                            it.users = newUsersOther
                        }
                        UserModel.current()?.let { newUsers.add(it) }
                    }

                    like.total = newUsers.size
                    like.users = newUsers
                    adapter.setList(say.likes)
                }
            }
        }

        holder.itemView.item_layout.isClickable = item.t?.say != null
        holder.itemView.item_dolike.visibility = if(holder.itemView.item_layout.isClickable) View.VISIBLE else View.INVISIBLE
        holder.itemView.item_dolike.setOnClickListener { v ->
            val say = item.t?.say?: return@setOnClickListener
            likePopup(v) { value ->
                (holder.itemView.context as BaseActivity).subscribe {
                    TopicPost.Like.dolike(50, 1, say.id.toString(), value)
                    val user = UserModel.current()?: return@subscribe
                    val like = say.likes?.firstOrNull{ it.value.toString() == value }
                    if(like == null) {
                        // 删除其他的，只能留下一个
                        say.likes?.forEach {
                            val newUsersOther = it.users.toMutableList()
                            newUsersOther.removeAll { it.username == user.username }
                            it.total = newUsersOther.size
                            it.users = newUsersOther
                        }
                        say.likes = say.likes.orEmpty() + TopicPost.Like(
                            value = value.toIntOrNull()?: 0,
                            type = 50,
                            main_id = 1,
                            total = 1,
                            users = listOf(user)
                        )
                    } else {
                        val newUsers = like.users.toMutableList()
                        if(!newUsers.removeAll { it.username == user.username }){
                            // 删除其他的，只能留下一个
                            say.likes?.forEach {
                                val newUsersOther = it.users.toMutableList()
                                newUsersOther.removeAll { it.username == user.username }
                                it.total = newUsersOther.size
                                it.users = newUsersOther
                            }
                            UserModel.current()?.let { newUsers.add(it) }
                        }
                        like.total = newUsers.size
                        like.users = newUsers
                    }
                    this@TimeLineAdapter.notifyItemChanged(holder.adapterPosition)
                }
            }
            (holder.itemView.context as BaseActivity).subscribe {

            }
        }
        //action
        holder.itemView.item_action.text = HtmlUtil.html2span(item.t?.action ?: "")
        holder.itemView.item_action.movementMethod =
            if (holder.itemView.item_layout.isClickable) null else LinkMovementMethod.getInstance()
        //del
        holder.itemView.item_del.visibility = if (item.t?.delUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        holder.itemView.item_del.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context).setMessage(R.string.timeline_dialog_remove)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    (holder.itemView.context as BaseActivity).subscribe(
                        key = item.t?.delUrl
                    ) {
                        TimeLine.removeTimeLine(item)
                        val index = holder.layoutPosition
                        val removeHeader =
                            getItemOrNull(index - 1)?.isHeader == true && getItemOrNull(index + 1)?.isHeader == true
                        removeAt(index)
                        if (removeHeader) removeAt(index - 1)
                    }
                }.show()
        }
        //collectStar
        holder.itemView.item_rate.visibility = if (item.t?.collectStar == 0) View.GONE else View.VISIBLE
        holder.itemView.item_rate.rating = (item.t?.collectStar ?: 0) * 0.5f
        //content
        holder.itemView.item_content.visibility = if (item.t?.content.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.itemView.item_content.text = item.t?.content ?: ""
        holder.itemView.item_content.setOnClickListener {
            WebActivity.launchUrl(holder.itemView.context, item.t?.contentUrl ?: return@setOnClickListener, "")
        }
        holder.itemView.item_content.isClickable = item.t?.contentUrl != null
        //time
        holder.itemView.item_time.text = item.t?.time

        val userImage = Images.getImage(item.t?.user?.avatar)
        holder.itemView.item_avatar.visibility =
            if (userImage.isEmpty() || getItemOrNull(holder.adapterPosition - 1)?.t?.user?.username == item.t?.user?.username) View.INVISIBLE else View.VISIBLE
        holder.itemView.item_avatar.setOnClickListener {
            WebActivity.launchUrl(holder.itemView.context, item.t?.user?.url, "")
        }
        if (userImage.isNotEmpty())
            GlideUtil.with(holder.itemView.item_avatar)
                ?.load(userImage)
                ?.apply(
                    RequestOptions.circleCropTransform().error(R.drawable.err_404)
                        .placeholder(R.drawable.placeholder_round)
                )
                ?.into(holder.itemView.item_avatar)
    }

}