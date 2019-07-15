package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.dialog_edit_progress.view.*
import okhttp3.FormBody
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class EditProgressDialog(context: Context): Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: Context, subject: Subject, formhash: String, ua: String, callback: ()->Unit){
            val dialog = EditProgressDialog(context)
            dialog.subject = subject
            dialog.formhash = formhash
            dialog.callback = callback
            dialog.ua = ua
            dialog.show()
        }
    }

    private fun getKeyBoardHeight(): Int{
        val rect = Rect()

        window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val metrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels - rect.bottom
    }

    lateinit var subject: Subject
    lateinit var formhash: String
    lateinit var ua: String
    lateinit var callback: ()->Unit
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_progress, null)
        setContentView(view)

        view.item_eps.setText(subject.ep_status.toString())
        view.item_eps_info.text = "${ if(subject.eps_count == 0) "" else "/${subject.eps_count}" } ${view.context.getString(R.string.ep_unit)}"
        view.item_vol.setText(subject.vol_status.toString())
        view.item_vol_info.text = "${ if(subject.vol_count == 0) "" else "/${subject.vol_count}" } ${view.context.getString(R.string.vol_unit)}"
        view.item_vol_panel.visibility = if(subject.has_vol) View.VISIBLE else View.GONE

        view.item_outside.setOnClickListener {
            dismiss()
        }
        view.item_submit.setOnClickListener {
            dismiss()
            val body = FormBody.Builder()
                    .add("referer", "subject")
                    .add("submit", "更新")
                    .add("watchedeps", view.item_eps.text.toString())
            if(subject.has_vol) body.add("watched_vols", view.item_vol.text.toString())

            ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/set/watched/${subject.id}", mapOf("User-Agent" to ua), body.build()){
                it.code() == 200
            }.enqueue(ApiHelper.buildCallback({
                if(!it) return@buildCallback
                subject.ep_status = view.item_eps.text.toString().toIntOrNull()?:0
                subject.vol_status = view.item_vol.text.toString().toIntOrNull()?:0
                callback()
            },{}))
        }

        window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener{
            view.item_keyboard.layoutParams?.let{
                it.height = getKeyBoardHeight()
                view.item_keyboard.layoutParams = it
            }
        }

        window?.attributes?.let{
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}