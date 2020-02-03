package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import kotlinx.android.synthetic.main.base_dialog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ThemeModel

/**
 * 基础对话框
 */
abstract class BaseDialog(context: Context, @LayoutRes private val resId: Int) :
    Dialog(context, R.style.AppTheme_Dialog) {
    abstract val title: String
    abstract fun onViewCreated(view: View)
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(resId, null)
        setContentView(view)
        onViewCreated(view)

        view.item_outside.setOnClickListener {
            if (!(context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
            ) dismiss()
        }
        view.btn_dismiss?.setOnClickListener {
            dismiss()
        }
        view.dialog_title?.text = title

        window?.let { ThemeModel.updateNavigationTheme(it, context) }

        window?.attributes?.let {
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}