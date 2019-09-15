package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TableRow
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_infobox.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.topic.PostAdapter
import soko.ekibun.bangumi.ui.web.WebActivity

class InfoboxDialog(context: Context): Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: Context, subject: Subject){
            if(subject.infobox?.isNotEmpty() != true) return
            val dialog = InfoboxDialog(context)
            dialog.subject = subject
            dialog.show()
        }
    }

    var subject: Subject = Subject()
    var callback: ((eps: List<Episode>, status: String)->Unit)? = null
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_infobox, null)
        setContentView(view)

        view.toolbar.title = subject.name
        view.toolbar_bottom.title = subject.name

        subject.infobox?.map{ it.first }?.distinct()?.forEach { cat ->
            val row = TableRow(context)
            val txtCat = TextView(context)
            txtCat.alpha = 0.8f
            txtCat.text = cat
            val txtInfo = TextView(context)
            @Suppress("DEPRECATION")
            txtInfo.text = PostAdapter.setTextLinkOpenByWebView(Html.fromHtml(subject.infobox?.filter { it.first == cat }?.map{ it.second }?.reduce { acc, s -> "$acc<br>$s" })){
                WebActivity.launchUrl(context, Bangumi.parseUrl(it), "")
            }
            txtInfo.movementMethod = LinkMovementMethod.getInstance()
            val dp = context.resources.displayMetrics.density
            txtInfo.setLineSpacing(4 * dp, 1f)
            txtInfo.setPadding((4 * dp + 0.5).toInt(), 0, 0, (6 * dp + 0.5).toInt())
            row.addView(txtCat)
            row.addView(txtInfo)
            view.item_infobox.addView(row)
        }

        val behavior = BottomSheetBehavior.from(view.bottom_sheet)
        behavior.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN)dismiss()
                view.app_bar.visibility = if(newState == BottomSheetBehavior.STATE_EXPANDED) View.VISIBLE else View.INVISIBLE
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        behavior.isHideable = true

        view.item_outside.setOnClickListener {
            dismiss()
        }
        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.bottom_sheet_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(view.bottom_sheet.paddingLeft, paddingTop + insets.systemWindowInsetTop, view.bottom_sheet.paddingRight, view.bottom_sheet.paddingBottom)
            view.bottom_sheet_container.setPadding(view.bottom_sheet_container.paddingLeft, view.bottom_sheet_container.paddingTop, view.bottom_sheet_container.paddingRight, paddingBottom + insets.systemWindowInsetBottom)
            insets
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
