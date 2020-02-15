package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_timeline.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.topic.ReplyDialog

/**
 * 时间线
 * @property titleRes Int
 * @property iconRes Int
 */
class TimeLineFragment : HomeTabFragment(R.layout.fragment_timeline) {
    override val titleRes: Int = R.string.timeline
    override val iconRes: Int = R.drawable.ic_timelapse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TimeLinePagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)
        val popup = PopupMenu(view.context, item_type)
        popup.menuInflater.inflate(R.menu.list_timeline, popup.menu)
        item_type?.text = popup.menu.findItem(adapter.selectedType)?.title
        item_type?.setOnClickListener {
            popup.setOnMenuItemClickListener {
                item_type?.text = it.title
                adapter.selectedType = it.itemId
                adapter.reset()
                true
            }
            popup.show()
        }

        var draft: String? = null
        item_new?.setOnClickListener {
            ReplyDialog.showDialog(
                activity?.supportFragmentManager ?: return@setOnClickListener,
                hint = context?.getString(R.string.timeline_dialog_add) ?: "",
                draft = draft
            ) { content, _, send ->
                if (content != null && send) {
                    TimeLine.addComment(content).enqueue(ApiHelper.buildCallback({
                        if (it) {
                            draft = null
                            if (item_pager?.currentItem ?: 2 !in 0..1) item_pager?.currentItem = 1
                            adapter.pageIndex[item_pager?.currentItem ?: 0] = 0
                            adapter.loadTopicList()
                        } else Snackbar.make(item_pager, R.string.hint_submit_error, Snackbar.LENGTH_SHORT).show()
                    }) { })
                } else draft = content
            }
        }

        onSelect()
    }

    override fun onUserChange() {
        val adapter = (item_pager?.adapter as? TimeLinePagerAdapter) ?: return
        val hasUser = (activity as? MainActivity)?.user != null
        if (hasUser) item_new?.show() else item_new?.hide()
        item_type?.visibility = if (hasUser) View.VISIBLE else View.GONE
        adapter.reset()
    }

    override fun onSelect() {
        val adapter = (item_pager?.adapter as? TimeLinePagerAdapter) ?: return
        adapter.pageIndex[item_pager?.currentItem ?: 0] = adapter.pageIndex[item_pager?.currentItem ?: 0] ?: 0
        if (adapter.pageIndex[item_pager?.currentItem ?: 0] == 0) adapter.loadTopicList()
    }
}