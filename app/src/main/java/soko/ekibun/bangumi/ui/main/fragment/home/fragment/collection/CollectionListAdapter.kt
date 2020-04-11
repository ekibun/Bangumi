package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 收藏列表Adapter
 * @constructor
 */
class CollectionListAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, item.displayName)
        helper.setText(R.id.item_name_jp, item.name)

        helper.itemView.item_onair.text = ""

        helper.setText(R.id.item_summary, when {
            item.ep_status == -1 -> {
                helper.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        helper.itemView.context,
                        android.R.attr.textColorSecondary
                    )
                )
                item.summary
            }
            item.type == Subject.TYPE_BOOK -> {
                val context = helper.itemView.context
                helper.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        helper.itemView.context,
                        if (item.vol_count != 0 && (item.vol_count <= 0 || item.vol_status != item.vol_count) || item.eps_count == 0 || item.ep_status != item.eps_count) R.attr.colorPrimary
                        else android.R.attr.textColorSecondary
                    )
                )
                context.getString(
                    R.string.phrase_progress,
                    (if (item.vol_count != 0) context.getString(
                        R.string.parse_sort_vol,
                        "${item.vol_status}${if (item.vol_count <= 0) "" else "/${item.vol_count}"}"
                    ) + " " else "") + context.getString(
                        R.string.parse_sort_ep,
                        "${item.ep_status}${if (item.eps_count <= 0) "" else "/${item.eps_count}"}"
                    )
                ) + (item.airInfo?.let { ", $it" } ?: "")
            }
            else -> {
                val eps = (item.eps as? List<*>)?.mapNotNull { it as? Episode }?.filter { it.type == Episode.TYPE_MAIN }
                val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
                val airTo = eps?.lastOrNull { it.isAir }
                eps?.getOrNull(watchTo?.let { eps.indexOf(it) + 1 } ?: 0)?.let { newEp ->
                    if (newEp.airdate == null) return@let
                    val use30h = App.app.sp.getBoolean("calendar_use_30h", false)
                    val dateTime =
                        (helper.itemView.context as MainActivity).mainPresenter.calendar.firstOrNull { it.id == item.id }
                            ?.getEpisodeDateTime(newEp) ?: return@let
                    val nowInt = CalendarAdapter.getNowInt(use30h)
                    helper.itemView.item_onair.text = if (dateTime.first == nowInt) dateTime.second
                    else if (dateTime.first > nowInt) {
                        val airDate = CalendarAdapter.getIntCalendar(dateTime.first)
                        val nowDate = CalendarAdapter.getIntCalendar(nowInt)
                        if (airDate.timeInMillis - nowDate.timeInMillis > 24 * 60 * 60 * 1000) {
//                            val week = CalendarAdapter.getWeek(CalendarAdapter.getIntCalendar(dateTime.first))
//                            TimeUtil.weekList[week] + dateTime.second
                            ""
                        } else "明天${dateTime.second}"
                    } else ""
                }
                helper.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        helper.itemView.context,
                        if (watchTo != airTo) R.attr.colorPrimary
                        else android.R.attr.textColorSecondary
                    )
                )
                (watchTo?.parseSort()?.let { helper.itemView.context.getString(R.string.parse_watch_to, it) }
                    ?: helper.itemView.context.getString(R.string.hint_watch_nothing)) +
                        when {
                            eps?.any { !it.isAir } == true -> airTo?.parseSort()
                                ?.let { " / " + helper.itemView.context.getString(R.string.parse_update_to, it) } ?: ""
                            item.eps_count > 0 -> " / " + helper.itemView.context.getString(
                                R.string.phrase_full_eps,
                                item.eps_count
                            )
                            else -> ""
                        }
            }
        })
        GlideUtil.with(helper.itemView.item_cover)
            ?.load(Images.getImage(item.image))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(helper.itemView.item_cover)
    }
}