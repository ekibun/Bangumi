package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_episode_list.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 剧集列表对话框
 * @property presenter SubjectPresenter
 * @property adapter EpisodeAdapter
 * @property callback Function2<[@kotlin.ParameterName] List<Episode>, [@kotlin.ParameterName] String, Unit>?
 * @property title String
 * @property clearSelection Function0<Unit>
 */
class EpisodeListDialog : BaseDialog(R.layout.dialog_episode_list) {
    companion object {
        /**
         * 显示对话框
         * @param fragmentManager FragmentManager
         * @param presenter SubjectPresenter
         */
        fun showDialog(fragmentManager: FragmentManager, presenter: SubjectPresenter) {
            val dialog = EpisodeListDialog()
            dialog.presenter = presenter
            dialog.show(fragmentManager, "ep list")
        }
    }

    lateinit var presenter: SubjectPresenter
    val adapter get() = presenter.subjectView.episodeDetailAdapter
    var callback: ((eps: List<Episode>, status: String) -> Unit)? = null
    override val title: String = ""

    val clearSelection = {
        adapter.data.forEach { it.isSelected = false }
        adapter.updateSelection()
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onViewCreated(view: View) {
        val behavior = BottomSheetBehavior.from(view.bottom_sheet)
        var offset = 0f
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { /* no-op */
                offset = slideOffset
            }
        })

        behavior.isHideable = false
        view.post {
            behavior.peekHeight = view.height * 2 / 3
            view.bottom_sheet_container.invalidate()
        }

        val nestedScrollRange = {
            view.bottom_sheet.height - behavior.peekHeight
        }
        val touchListener = adapter.setUpWithRecyclerView(view.shc, view.bottom_sheet_container)
        touchListener.nestScrollDistance = {
            (nestedScrollRange() * (1 - offset)).toInt()
        }
        view.bottom_sheet_container.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        view.bottom_sheet_container.scrollTopMargin = ResourceUtil.toPixels(view.context.resources, 36f)
        view.bottom_sheet_container.nestedScrollDistance = {
            (nestedScrollRange() * offset).toInt()
        }
        view.bottom_sheet_container.nestedScrollRange = nestedScrollRange

        val progressMap =
            arrayOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_QUEUE, Episode.PROGRESS_DROP, Episode.PROGRESS_REMOVE)

        view.btn_edit_ep.setOnClickListener {
            val eps = adapter.data.filter { it.isSelected }
            if (eps.isEmpty()) return@setOnClickListener
            val epStatus = view.context.resources.getStringArray(R.array.episode_status).toMutableList()
            epStatus.removeAt(1)

            val popupMenu = PopupMenu(view.context, view.btn_edit_ep)
            epStatus.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s)
            }
            popupMenu.setOnMenuItemClickListener { menu ->
                val newStatus = progressMap[menu.itemId - Menu.FIRST]
                presenter.updateProgress(eps.map { it.t }, newStatus)
                false
            }
            popupMenu.show()
        }
        adapter.updateSelection = {
            val eps = adapter.data.filter { it.isSelected }
            view.btn_edit_ep.visibility = if (eps.isEmpty()) View.GONE else View.VISIBLE
            view.item_ep_title.visibility = view.btn_edit_ep.visibility
            view.item_ep_title.text =
                "${view.context.getText(R.string.episodes)}${if (eps.isEmpty()) "" else "(${eps.size})"}"
        }

        adapter.setOnItemChildLongClickListener { _, _, position ->
            val ep = adapter.data[position]?.t
            if (ep?.type == Episode.TYPE_MUSIC) {
                presenter.showEpisodeDialog(ep.id)
                true
            } else adapter.longClickListener(position)
        }

        adapter.setOnItemChildClickListener { _, _, position ->
            val ep = adapter.data[position]?.t
            if (ep?.type == Episode.TYPE_MUSIC || adapter.clickListener(position))
                ep?.let { presenter.showEpisodeDialog(it.id) }
        }

        view.btn_dismiss.setOnClickListener {
            if (adapter.data.none { it.isSelected }) dismiss()
            else clearSelection()
        }

        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.bottom_sheet_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(
                view.bottom_sheet.paddingLeft,
                paddingTop + insets.systemWindowInsetTop,
                view.bottom_sheet.paddingRight,
                view.bottom_sheet.paddingBottom
            )
            view.bottom_sheet_container.setPadding(
                view.bottom_sheet_container.paddingLeft,
                view.bottom_sheet_container.paddingTop,
                view.bottom_sheet_container.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        clearSelection()
    }
}
