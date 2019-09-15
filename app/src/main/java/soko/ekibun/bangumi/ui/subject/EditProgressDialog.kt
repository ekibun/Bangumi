package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.dialog_edit_progress.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel

class EditProgressDialog(context: Context): Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: Context, subject: Subject, callback: () -> Unit) {
            val dialog = EditProgressDialog(context)
            dialog.subject = subject
            dialog.callback = callback
            dialog.show()
        }
    }

    lateinit var subject: Subject
    lateinit var callback: ()->Unit
    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_progress, null)
        setContentView(view)

        view.item_eps.setText(subject.ep_status.toString())
        view.item_eps_info.text = "${if (subject.eps_count <= 0) "" else "/${subject.eps_count}"} ${view.context.getString(R.string.ep_unit)}"
        view.item_vol.setText(subject.vol_status.toString())
        view.item_vol_info.text = "${if (subject.vol_count <= 0) "" else "/${subject.vol_count}"} ${view.context.getString(R.string.vol_unit)}"
        view.item_vol_panel.visibility = if (subject.vol_count != 0) View.VISIBLE else View.GONE

        view.item_outside.setOnClickListener {
            dismiss()
        }
        view.item_submit.setOnClickListener {
            dismiss()
            Bangumi.updateSubjectProgress(subject, view.item_eps.text.toString(), view.item_vol.text.toString()).enqueue(ApiHelper.buildCallback({
                if(!it) return@buildCallback
                subject.ep_status = view.item_eps.text.toString().toIntOrNull()?:0
                subject.vol_status = view.item_vol.text.toString().toIntOrNull()?:0
                callback()
            },{}))
        }

        val paddingBottom = view.item_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_container.setPadding(view.item_container.paddingLeft, view.item_container.paddingTop, view.item_container.paddingRight, paddingBottom + insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }

        window?.let { ThemeModel.updateNavigationTheme(it, view.context) }

        window?.attributes?.let{
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}