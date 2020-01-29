package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.dialog_edit_progress.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.BaseDialog

/**
 * 进度编辑对话框
 */
class EditProgressDialog(context: Context) : BaseDialog(context, R.layout.dialog_edit_progress) {
    companion object {
        /**
         * 显示对话框
         */
        fun showDialog(context: Context, subject: Subject, callback: () -> Unit) {
            val dialog = EditProgressDialog(context)
            dialog.subject = subject
            dialog.callback = callback
            dialog.show()
        }
    }

    lateinit var subject: Subject
    lateinit var callback: () -> Unit
    override val title: String = "修改进度"

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onViewCreated(view: View) {
        view.item_eps.setText(subject.ep_status.toString())
        view.item_eps_info.text =
            "${if (subject.eps_count <= 0) "" else "/${subject.eps_count}"} ${view.context.getString(R.string.ep_unit)}"
        view.item_vol.setText(subject.vol_status.toString())
        view.item_vol_info.text =
            "${if (subject.vol_count <= 0) "" else "/${subject.vol_count}"} ${view.context.getString(R.string.vol_unit)}"
        view.item_vol_panel.visibility = if (subject.vol_count != 0) View.VISIBLE else View.GONE

        view.item_submit.setOnClickListener {
            dismiss()
            Subject.updateSubjectProgress(subject, view.item_eps.text.toString(), view.item_vol.text.toString())
                .enqueue(ApiHelper.buildCallback({
                    if (!it) return@buildCallback
                subject.ep_status = view.item_eps.text.toString().toIntOrNull()?:0
                subject.vol_status = view.item_vol.text.toString().toIntOrNull()?:0
                callback()
            },{}))
        }

        val paddingBottom = view.item_buttons.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_buttons.setPadding(
                view.item_buttons.paddingLeft,
                view.item_buttons.paddingTop,
                view.item_buttons.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }
}