package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.base_dialog.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.ThemeModel

/**
 * 基础对话框
 */
abstract class BaseDialog(@LayoutRes private val resId: Int) :
    DialogFragment() {
    var contentView: View? = null
    abstract val title: String
    abstract fun onViewCreated(view: View)
    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        val view = contentView ?: inflater.inflate(resId, null)
        contentView = view

        view.item_outside?.setOnClickListener {
            if (!(it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
            ) dismiss()
        }
        view.btn_dismiss?.setOnClickListener {
            dismiss()
        }
        view.dialog_title?.text = title

        onViewCreated(view)

        dialog?.window?.let { ThemeModel.updateNavigationTheme(it, it.context) }

        dialog?.window?.attributes?.let {
            it.dimAmount = 0.6f
            dialog?.window?.attributes = it
        }
        dialog?.window?.setWindowAnimations(R.style.AnimDialog)
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }
}