package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_episode_list.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 剧集列表对话框
 */
class EpisodeListDialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        /**
         * 显示对话框
         */
        fun showDialog(context: Context, presenter: SubjectPresenter) {
            val dialog = EpisodeListDialog(context)
            dialog.presenter = presenter
            dialog.show()
        }
    }

    lateinit var presenter: SubjectPresenter
    val adapter get() = presenter.subjectView.episodeDetailAdapter
    var callback: ((eps: List<Episode>, status: String) -> Unit)? = null
    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_episode_list, null)
        setContentView(view)

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
        val touchListener = adapter.setUpWithRecyclerView(view.bottom_sheet_container)
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
            val epStatus = context.resources.getStringArray(R.array.episode_status).toMutableList()
            epStatus.removeAt(1)

            val popupMenu = PopupMenu(context, view.btn_edit_ep)
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
                "${context.getText(R.string.episodes)}${if (eps.isEmpty()) "" else "(${eps.size})"}"
        }

        adapter.setOnItemChildLongClickListener { _, _, position ->
            val eps =
                adapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            if (eps.last().type == Episode.TYPE_MUSIC)
                adapter.data[position]?.t?.let {
                    presenter.openEpisode(it, eps)
                    true
                } ?: false
            else adapter.longClickListener(position)
        }

        adapter.setOnItemChildClickListener { _, _, position ->
            val eps =
                adapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            if (eps.last().type == Episode.TYPE_MUSIC || adapter.clickListener(position))
                adapter.data[position]?.t?.let { presenter.openEpisode(it, eps) }
        }

        val clearSelection = {
            adapter.data.forEach { it.isSelected = false }
            adapter.updateSelection()
            adapter.notifyDataSetChanged()
        }

        view.btn_dismiss.setOnClickListener {
            if (adapter.data.none { it.isSelected }) dismiss()
            else clearSelection()
        }
        view.item_outside.setOnClickListener {
            dismiss()
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

        setOnDismissListener {
            clearSelection()
        }

        window?.let { ThemeModel.updateNavigationTheme(it, view.context) }

        window?.attributes?.let {
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}
